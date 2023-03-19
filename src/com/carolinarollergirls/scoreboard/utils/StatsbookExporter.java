package com.carolinarollergirls.scoreboard.utils;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Expulsion;
import com.carolinarollergirls.scoreboard.core.interfaces.Fielding;
import com.carolinarollergirls.scoreboard.core.interfaces.FloorPosition;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Jam;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.Penalty;
import com.carolinarollergirls.scoreboard.core.interfaces.Period;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoringTrip;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.core.interfaces.TeamJam;
import com.carolinarollergirls.scoreboard.core.interfaces.Timeout;

public class StatsbookExporter extends Thread {
    public StatsbookExporter(Game g) {
        game = g;
        coreLock = GameImpl.getCoreLock();
        start();
    }

    public static void preload(String blankStatsbookPath) {
        if ("".equals(blankStatsbookPath)) { return; }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream in = new FileInputStream(Paths.get(blankStatsbookPath).toFile());
                    Workbook wb = WorkbookFactory.create(in);
                    in.close();

                    Sheet igrf = wb.getSheet("IGRF");
                    Font strikeFont = wb.createFont();
                    strikeFont.setStrikeout(true);
                    CellStyle strikedNum = wb.createCellStyle();
                    strikedNum.cloneStyleFrom(igrf.getRow(13).getCell(1).getCellStyle());
                    strikedNum.setFont(strikeFont);

                    Cell cell = igrf.getRow(13).getCell(2);
                    cell.setCellValue("Name");
                    setComment(cell, "Comment");

                    cell = wb.getSheet("Score").getRow(4).getCell(10);
                    List<Integer> values = Arrays.asList(4, 4, 4);
                    cell.setCellFormula(values.stream().map(String::valueOf).collect(Collectors.joining("+")));

                    Path tmpPath = Paths.get("config/~tmp.xlsx");
                    FileOutputStream out = new FileOutputStream(tmpPath.toFile());
                    wb.write(out);
                    out.close();
                    wb.close();

                    Files.delete(tmpPath);
                } catch (Exception e) { Logger.printStackTrace(e); }
            }
        }).start();
    }

    @Override
    public void run() {
        boolean success = false;
        try {
            String blankStatsbookPath = game.getScoreBoard().getSettings().get(ScoreBoard.SETTING_STATSBOOK_INPUT);
            if (!"".equals(blankStatsbookPath)) {
                Path tmpPath = BasePath.get().toPath().resolve("html/game-data/xlsx/~" + game.getFilename() + ".xlsx");
                Path fullPath = BasePath.get().toPath().resolve("html/game-data/xlsx/" + game.getFilename() + ".xlsx");
                Files.copy(Paths.get(blankStatsbookPath), tmpPath, REPLACE_EXISTING);
                FileInputStream in = new FileInputStream(tmpPath.toFile());
                wb = WorkbookFactory.create(in);
                in.close();

                synchronized (coreLock) { fillIgrfAndPenalties(); }
                synchronized (coreLock) {
                    fillScoreLineupsAndClock();
                    if (hadOsOffset) { fillIgrfOsOffsetInfo(); }
                }
                wb.setForceFormulaRecalculation(true);

                FileOutputStream out = new FileOutputStream(tmpPath.toFile());
                wb.write(out);
                out.close();
                wb.close();

                Files.move(tmpPath, fullPath, REPLACE_EXISTING);
            }
            success = true;
        } catch (Exception e) { Logger.printStackTrace(e); } finally {
            game.exportDone(success);
        }
    }

    private void fillIgrfAndPenalties() {
        Sheet igrf = wb.getSheet("IGRF");

        createCellStyles(igrf);

        fillIgrfHead(igrf);
        fillExpulsionSuspensionInfo(igrf);
        fillNsos(igrf);
        fillRefs(igrf);

        Sheet penalties = wb.getSheet("Penalties");
        Sheet clock = wb.getSheet("Game Clock");
        Sheet box = wb.getSheet("Penalty Box");

        fillTeamData(igrf, clock, Team.ID_1);
        fillTeamData(igrf, clock, Team.ID_2);
        fillPenaltiesHead(penalties);
        fillBoxHead(box);

        for (Team t : game.getAll(Game.TEAM)) {
            List<Skater> skaters = new ArrayList<>(t.getAll(Team.SKATER));
            Collections.sort(skaters, new Comparator<Skater>() {
                @Override
                public int compare(Skater s1, Skater s2) {
                    if (s1 == s2) { return 0; }
                    if (s1 == null) { return 1; }
                    return s1.compareTo(s2);
                }
            });
            int igrfRowId = 13;
            int penRowId = 3;
            for (Skater s : skaters) {
                fillSkater(igrf.getRow(igrfRowId), s, clock);
                fillPenalties(penalties.getRow(penRowId), penalties.getRow(penRowId + 1), s);
                if (!s.getFlags().startsWith("B")) {
                    igrfRowId++;
                    penRowId += 2;
                }
                if (igrfRowId > 32) { break; } // no more space
            }
        }
    }

    private void fillIgrfHead(Sheet igrf) {
        Row row = igrf.getRow(2);
        setEventInfoCell(row, 1, Game.INFO_VENUE);
        setEventInfoCell(row, 8, Game.INFO_CITY);
        setEventInfoCell(row, 10, Game.INFO_STATE);
        setEventInfoCell(row, 11, Game.INFO_GAME_NUMBER);
        row = igrf.getRow(4);
        setEventInfoCell(row, 1, Game.INFO_TOURNAMENT);
        setEventInfoCell(row, 8, Game.INFO_HOST);
        row = igrf.getRow(6);
        try {
            LocalDate date = LocalDate.parse(game.get(Game.EVENT_INFO, Game.INFO_DATE).getValue());
            row.getCell(1).setCellValue(date);
            LocalTime time = LocalTime.parse(game.get(Game.EVENT_INFO, Game.INFO_START_TIME).getValue());
            row.getCell(8).setCellValue(LocalDateTime.of(date, time));
        } catch (Exception e) {} // when parsing fails just leave them empty
    }

    private void fillExpulsionSuspensionInfo(Sheet igrf) {
        boolean suspension = !"".equals(game.get(Game.SUSPENSIONS_SERVED));
        Row row = igrf.getRow(39);
        if (suspension) { row.getCell(4).setCellValue(game.get(Game.SUSPENSIONS_SERVED)); }
        int rowId = 40;

        for (Expulsion e : game.getAll(Game.EXPULSION)) {
            suspension = suspension || e.get(Expulsion.SUSPENSION);
            row = igrf.getRow(rowId);
            row.getCell(0).setCellValue(e.get(Expulsion.INFO) + " " + e.get(Expulsion.EXTRA_INFO));
            row.getCell(11).setCellValue(e.get(Expulsion.SUSPENSION) ? "YES" : "NO");

            rowId += 2;
            if (rowId > 45) { break; } // no more space
        }
        igrf.getRow(6).getCell(11).setCellValue(suspension ? "YES" : "NO");
    }

    private void fillIgrfOsOffsetInfo() {
        if (hadOsOffset) {
            Row row = wb.getSheet("IGRF").getRow(38);
            row.getCell(3).setCellValue("yes");
            row.getCell(8).setCellValue(String.join(", ", osOffsetReasons));
        }
    }

    private void fillNsos(Sheet igrf) {
        if (game.get(Game.HEAD_NSO) != null) { fillOfficialRow(igrf.getRow(59), game.get(Game.HEAD_NSO), true); }

        List<Official> nsos = new ArrayList<>(game.getAll(Game.NSO));
        Collections.sort(nsos, new Comparator<Official>() {
            @Override
            public int compare(Official o1, Official o2) {
                if (o1 == o2) { return 0; }
                if (o1 == null) { return 1; }
                return o1.compareTo(o2);
            }
        });
        int rowId = 60;
        for (Official o : nsos) {
            fillOfficialRow(igrf.getRow(rowId), o);
            String name = o.get(Official.NAME);
            int tId = -1;
            Team t = o.get(Official.P1_TEAM);
            if (t != null) { tId = Integer.parseInt(t.getProviderId()) - 1; }
            switch (o.get(Official.ROLE)) {
            case Official.ROLE_PLT:
                pt = ("".equals(pt) ? "" : pt + " / ") + name;
                if (tId >= 0) {
                    lt[0][tId] = name;
                    lt[1][o.get(Official.SWAP) ? 1 - tId : tId] = name;
                }
                break;
            case Official.ROLE_PT: pt = o.get(Official.NAME); break;
            case Official.ROLE_SK:
                if (tId >= 0) {
                    sk[0][tId] = name;
                    sk[1][o.get(Official.SWAP) ? 1 - tId : tId] = name;
                }
                break;
            case Official.ROLE_LT:
                if (tId >= 0) {
                    lt[0][tId] = name;
                    lt[1][o.get(Official.SWAP) ? 1 - tId : tId] = name;
                }
                break;
            case Official.ROLE_PBT:
                if (tId >= 0) {
                    pbt[0][tId] = name;
                    pbt[1][o.get(Official.SWAP) ? 1 - tId : tId] = name;
                }
                break;
            default: break;
            }

            if (++rowId > 78) { break; } // we ran over the end of the table space
        }
    }

    private void fillRefs(Sheet igrf) {
        List<Official> refs = new ArrayList<>(game.getAll(Game.REF));
        Collections.sort(refs, new Comparator<Official>() {
            @Override
            public int compare(Official o1, Official o2) {
                if (o1 == o2) { return 0; }
                if (o1 == null) { return 1; }
                return o1.compareTo(o2);
            }
        });
        int rowId = game.get(Game.HEAD_REF) == null ? 80 : 79;
        for (Official o : refs) {
            fillOfficialRow(igrf.getRow(rowId), o);

            if (Official.ROLE_JR.equals(o.get(Official.ROLE))) {
                Team t = o.get(Official.P1_TEAM);
                if (t != null) {
                    int tId = Integer.parseInt(t.getProviderId()) - 1;
                    jr[0][tId] = o.get(Official.NAME);
                    jr[1][o.get(Official.SWAP) ? 1 - tId : tId] = o.get(Official.NAME);
                }
            }

            if (++rowId > 87) { break; }
        }
    }

    private void fillOfficialRow(Row row, Official o) { fillOfficialRow(row, o, false); }
    private void fillOfficialRow(Row row, Official o, boolean skipRole) {
        if (!skipRole) { setCell(row, 0, o.get(Official.ROLE)); }
        setCell(row, 2, o.get(Official.NAME));
        setCell(row, 7, o.get(Official.LEAGUE));
        setCell(row, 10, o.get(Official.CERT));
    }

    private void fillTeamData(Sheet igrf, Sheet clock, String teamId) {
        Team t = game.getTeam(teamId);
        int col = Team.ID_1.equals(teamId) ? 1 : 8;
        setCell(igrf.getRow(9), col, t.get(Team.LEAGUE_NAME));
        setCell(igrf.getRow(10), col, t.get(Team.TEAM_NAME));
        setCell(igrf.getRow(11), col, t.get(Team.UNIFORM_COLOR));
        String captain = t.get(Team.CAPTAIN) == null ? "" : t.get(Team.CAPTAIN).get(Skater.NAME);
        setCell(igrf.getRow(48), col, captain);
        setCell(clock.getRow(Team.ID_1.equals(teamId) ? 4 : 6), 1, captain);
        setCell(clock.getRow(Team.ID_1.equals(teamId) ? 55 : 57), 1, captain);
    }

    private void fillPenaltiesHead(Sheet penalties) {
        Row row = penalties.getRow(0);
        setCell(row, 13, pt);
        setCell(row, 41, pt);
    }

    private void fillBoxHead(Sheet box) {
        Row row = box.getRow(0);
        setCell(row, 11, pbt[0][0]);
        setCell(row, 28, pbt[0][1]);
        row = box.getRow(43);
        setCell(row, 11, pbt[1][0]);
        setCell(row, 28, pbt[1][1]);
    }

    private void fillSkater(Row row, Skater s, Sheet clock) {
        String teamId = s.getTeam().getProviderId();
        String flags = s.getFlags();
        if ("A".equals(flags) || "BA".equals(flags)) {
            setCell(clock.getRow(Team.ID_1.equals(teamId) ? 5 : 7), 1, s.getName());
            setCell(clock.getRow(Team.ID_1.equals(teamId) ? 56 : 58), 1, s.getName());
        }
        if ("ALT".equals(flags)) {
            setCell(row, Team.ID_1.equals(teamId) ? 1 : 8, s.getRosterNumber() + "*", strikedNum);
            setCell(row, Team.ID_1.equals(teamId) ? 2 : 9, s.getName(), strikedName);
        } else if (!flags.startsWith("B")) {
            setCell(row, Team.ID_1.equals(teamId) ? 1 : 8, s.getRosterNumber());
            setCell(row, Team.ID_1.equals(teamId) ? 2 : 9, s.getName());
        }
    }

    private void fillPenalties(Row penRow, Row jamRow, Skater s) {
        for (Penalty p : s.getAll(Skater.PENALTY)) {
            int num = p.getNumber();
            int period = p.getPeriodNumber();
            if (num > 9 || period < 1 || period > 2) { continue; }
            int col = num == 0 ? 10 : num;
            if (period == 2) { col += 28; }
            if (Team.ID_2.equals(s.getTeam().getProviderId())) { col += 15; }
            setCell(penRow, col, p.get(Penalty.CODE));
            setCell(jamRow, col, p.getJamNumber());
        }
    }

    private void fillScoreLineupsAndClock() {
        Sheet score = wb.getSheet("Score");
        Sheet osOffset = wb.getSheet("OS Offset");
        Sheet lineups = wb.getSheet("Lineups");
        Sheet clock = wb.getSheet("Game Clock");
        int[] toCols = {3, 3};

        for (int pn = 0; pn < game.getCurrentPeriodNumber(); pn++) {
            int rowIndex = pn * 42;
            fillScoreHead(score.getRow(rowIndex), pn);
            fillLineupsHead(lineups.getRow(rowIndex), pn);

            Period p = game.get(Game.PERIOD, pn + 1);
            toCols = fillTimeouts(clock, toCols, p);

            if (p.getCurrentJam().getPeriod() != p) {
                // period has no jams
                continue;
            }

            rowIndex += 3;
            for (int jn = 1; jn <= p.getCurrentJamNumber(); jn++) {
                fillJam(score.getRow(rowIndex), score.getRow(rowIndex + 1), osOffset.getRow(rowIndex),
                        lineups.getRow(rowIndex), lineups.getRow(rowIndex + 1), clock.getRow(pn * 51 + jn + 9),
                        p.getJam(jn));
                rowIndex += p.getJam(jn).get(Jam.STAR_PASS) ? 2 : 1;
                if (rowIndex > 82 || (rowIndex > 40 && rowIndex < 45)) { break; } // end of sheet
            }
        }
    }

    private void fillScoreHead(Row row, int period) {
        setCell(row, 11, sk[period][0]);
        setCell(row, 30, sk[period][1]);
        setCell(row, 14, jr[period][0]);
        setCell(row, 33, jr[period][1]);
    }

    private void fillLineupsHead(Row row, int period) {
        setCell(row, 15, lt[period][0]);
        setCell(row, 41, lt[period][1]);
    }

    private int[] fillTimeouts(Sheet clockSheet, int[] toCol, Period p) {
        int[] orCol = {6, 6};
        int baseRow = p.getNumber() == 1 ? 0 : 51;
        Row[] toRows = {clockSheet.getRow(baseRow + 4), clockSheet.getRow(baseRow + 6)};

        List<Timeout> timeouts = new ArrayList<>(p.getAll(Period.TIMEOUT));
        Collections.sort(timeouts, new Comparator<Timeout>() {
            @Override
            public int compare(Timeout t1, Timeout t2) {
                if (t1 == t2) { return 0; }
                if (t1 == null) { return 1; }
                return t1.compareTo(t2);
            }
        });

        for (Timeout t : timeouts) {
            String endTime =
                ClockConversion.toHumanReadable(t.get(Timeout.PRECEDING_JAM).get(Jam.PERIOD_CLOCK_DISPLAY_END));
            if (t.getOwner() instanceof Team) {
                int i = (t.getOwner() == game.get(Game.TEAM, Team.ID_1) ? 0 : 1);
                Row toRow = toRows[i];
                if (t.isReview()) {
                    if (orCol[i] <= 7) {
                        setCell(toRow, orCol[i], endTime);
                        if (orCol[i] == 6 && !t.isRetained()) {
                            setCell(toRow, orCol[i], "X");
                            orCol[i]++;
                        }
                        orCol[i]++;
                    }
                } else {
                    if (toCol[i] <= 5) {
                        setCell(toRow, toCol[i], endTime);
                        toCol[i]++;
                    }
                }
            }
        }

        if (p.getNumber() == 1) { // cross off timeouts for P2
            for (int i = 1; i <= 2; i++) {
                Row row = clockSheet.getRow(53 + i * 2);
                for (int col = 3; col < toCol[i - 1]; col++) { setCell(row, col, "X"); }
            }
        }

        return toCol;
    }

    private void fillJam(Row scoreRow, Row scoreSpRow, Row osOffsetRow, Row lineupsRow, Row lineupsSpRow, Row clockRow,
                         Jam j) {
        injuries = new ArrayList<>();

        TeamJam tj = j.getTeamJam(Team.ID_1);
        fillScoreTeamJam(scoreRow, scoreSpRow, 0, tj);
        fillOsOffsetTeamJam(osOffsetRow, 0, tj);
        fillLineupsTeamJam(lineupsRow, lineupsSpRow, 0, tj);

        tj = j.getTeamJam(Team.ID_2);
        fillScoreTeamJam(scoreRow, scoreSpRow, 19, tj);
        fillOsOffsetTeamJam(osOffsetRow, 7, tj);
        fillLineupsTeamJam(lineupsRow, lineupsSpRow, 26, tj);

        fillClockJam(clockRow, j);
    }

    private void fillScoreTeamJam(Row baseRow, Row spRow, int baseCol, TeamJam tj) {
        if (tj.getJam().isInjuryContinuation()) {
            setCell(baseRow, baseCol, "INJ" + (tj.isLead() ? "*" : ""));
        } else {
            setCell(baseRow, baseCol, tj.getJam().getNumber(), tj.getJam().isOvertimeJam() ? "Overtime Jam" : "");
        }
        if (tj.getJam().get(Jam.STAR_PASS)) { setCell(spRow, baseCol, tj.isStarPass() ? "SP" : "SP*"); }
        setCell(baseRow, baseCol + 1, tj.getFielding(FloorPosition.JAMMER).get(Fielding.SKATER_NUMBER));
        if (tj.get(TeamJam.STAR_PASS)) {
            setCell(spRow, baseCol + 1, tj.getFielding(FloorPosition.PIVOT).get(Fielding.SKATER_NUMBER));
        }

        setCell(baseRow, baseCol + 2, tj.isLost() ? "X" : "");
        setCell(baseRow, baseCol + 3, tj.isLead() ? "X" : "");
        setCell(baseRow, baseCol + 4, tj.isCalloff() ? "X" : "");
        setCell(baseRow, baseCol + 5, tj.isInjury() ? "X" : "");
        ScoringTrip processedTrip = fillInitialTrip(baseRow, spRow, baseCol + 6, tj.getFirst(TeamJam.SCORING_TRIP));
        while (processedTrip.hasNext() && processedTrip.getNumber() < 9) {
            processedTrip = fillTrip(baseRow, spRow, baseCol + 6 + processedTrip.getNumber(), processedTrip.getNext());
        }
        if (processedTrip.hasNext()) {
            processedTrip = fillLastTrips(baseRow, baseCol + 15, processedTrip, false);
            fillLastTrips(spRow, baseCol + 15, processedTrip, true);
        }
    }

    private ScoringTrip fillInitialTrip(Row baseRow, Row spRow, int initialCol, ScoringTrip initialTrip) {
        Row initialRow = baseRow;
        if (initialTrip.getNumber() > 1) {
            // injury continuation jam
            return fillTrip(baseRow, spRow, initialCol + initialTrip.getNumber() - 1, initialTrip);
        }
        if (initialTrip.isAfterSP()) {
            setCell(baseRow, initialCol, "X");
            initialRow = spRow;
        }
        setCell(initialRow, initialCol, initialTrip.hasNext() ? "" : "X", initialTrip.getAnnotation());

        if (initialTrip.getScore() == 0) {
            return initialTrip;
        } else if (!initialTrip.hasNext()) {
            setCell(initialRow, initialCol + 1, String.valueOf(initialTrip.getScore()) + " + NI");
            setCell(initialRow, initialCol + 10, initialTrip.getScore()); // Jam total formula has to be overwritten
            return initialTrip;
        } else {
            ScoringTrip firstScoringTrip = initialTrip.getNext();
            if (initialTrip.isAfterSP() != firstScoringTrip.isAfterSP()) {
                setCell(initialRow, initialCol + 1, String.valueOf(initialTrip.getScore()) + " + SP");
                setCell(initialRow, initialCol + 10, initialTrip.getScore()); // Jam total formula has to be
                                                                              // overwritten
                return initialTrip;
            } else {
                List<Integer> points = Arrays.asList(initialTrip.getScore(), firstScoringTrip.getScore());
                List<String> comments = new ArrayList<>();
                if (!"".equals(firstScoringTrip.getAnnotation())) { comments.add(firstScoringTrip.getAnnotation()); }
                setCell(initialRow, initialCol + 1, points, comments);
                return firstScoringTrip;
            }
        }
    }

    private ScoringTrip fillTrip(Row baseRow, Row spRow, int col, ScoringTrip trip) {
        setCell(trip.isAfterSP() ? spRow : baseRow, col, trip.getScore(), trip.getAnnotation());
        return trip;
    }

    private ScoringTrip fillLastTrips(Row row, int col, ScoringTrip trip, boolean afterSp) {
        List<Integer> points = new ArrayList<>();
        List<String> comments = new ArrayList<>();

        while (trip.hasNext() && trip.getNext().isAfterSP() == afterSp) {
            trip = trip.getNext();
            points.add(trip.getScore());
            if (!"".equals(trip.getAnnotation())) {
                comments.add("T" + trip.getNumber() + ": " + trip.getAnnotation());
            }
        }

        setCell(row, col, points, comments);
        return trip;
    }

    private void fillOsOffsetTeamJam(Row osOffsetRow, int startCol, TeamJam tj) {
        if (tj.getOsOffset() != 0) {
            setCell(osOffsetRow, startCol + 1, tj.getOsOffset());
            setCell(osOffsetRow, startCol + 2, tj.get(TeamJam.OS_OFFSET_REASON));
            hadOsOffset = true;
            osOffsetReasons.add(tj.get(TeamJam.OS_OFFSET_REASON));
        }
    }

    private void fillLineupsTeamJam(Row baseRow, Row spRow, int c, TeamJam tj) {
        if (!tj.getJam().isInjuryContinuation() || !tj.isLead()) {
            setCell(baseRow, c + 1, tj.hasNoPivot() ? "X" : "");
            fillFielding(baseRow, c + 2, tj.getFielding(FloorPosition.JAMMER), false, true);
            fillFielding(baseRow, c + 6, tj.getFielding(FloorPosition.PIVOT), false);
            fillFielding(baseRow, c + 10, tj.getFielding(FloorPosition.BLOCKER1), false);
            fillFielding(baseRow, c + 14, tj.getFielding(FloorPosition.BLOCKER2), false);
            fillFielding(baseRow, c + 18, tj.getFielding(FloorPosition.BLOCKER3), false);
        }
        if (tj.isStarPass()) {
            spRow.getCell(c + 1).setCellValue("X");
            fillFielding(spRow, c + 2, tj.getFielding(FloorPosition.PIVOT), true, true);
            fillFielding(spRow, c + 6, tj.getFielding(FloorPosition.JAMMER), true);
            fillFielding(spRow, c + 10, tj.getFielding(FloorPosition.BLOCKER1), true);
            fillFielding(spRow, c + 14, tj.getFielding(FloorPosition.BLOCKER2), true);
            fillFielding(spRow, c + 18, tj.getFielding(FloorPosition.BLOCKER3), true);
        }
    }

    private void fillFielding(Row row, int startCol, Fielding f, boolean afterSp) {
        fillFielding(row, startCol, f, afterSp, false);
    }
    private void fillFielding(Row row, int startCol, Fielding f, boolean afterSp, boolean skipNumber) {
        if (!skipNumber) { setCell(row, startCol, f.get(Fielding.SKATER_NUMBER), f.get(Fielding.ANNOTATION)); }
        String[] boxSyms =
            f.get(afterSp ? Fielding.BOX_TRIP_SYMBOLS_AFTER_S_P : Fielding.BOX_TRIP_SYMBOLS_BEFORE_S_P).split(" ");
        for (int i = 0; i < boxSyms.length; i++) {
            setCell(row, startCol + i + 1, boxSyms[i]);
            if ("3".equals(boxSyms[i])) {
                injuries.add(f.getPosition().getTeam().get(Team.UNIFORM_COLOR) + " " + f.get(Fielding.SKATER_NUMBER));
            }
        }
    }

    private void fillClockJam(Row row, Jam j) {
        List<String> events = new ArrayList<>();
        List<String> eventDetails = new ArrayList<>();
        boolean bAddTimeToDetails = false;
        String endTime = ClockConversion.toHumanReadable(j.get(Jam.PERIOD_CLOCK_DISPLAY_END));

        setCell(row, 1, j.getDuration() / 1000);
        for (Penalty pen : j.getAll(Jam.PENALTY)) {
            if (Skater.FO_EXP_ID.equals(pen.getProviderId()) && !"FO".equals(pen.getCode())) { // expulsion
                events.add("EXP");
                eventDetails.add(pen.getParent().getParent().get(Team.UNIFORM_COLOR) + " " +
                                 pen.getParent().get(Skater.ROSTER_NUMBER));
            }
        }
        if (j.getTeamJam(Team.ID_1).isInjury()) { // flag is set for both or neither team
            events.add("INJ");
            eventDetails.add(String.join(", ", injuries));
        }
        for (Timeout t : j.getAll(Jam.TIMEOUTS_AFTER)) {
            if (t.getOwner() instanceof Team) {
                events.add(t.isReview() ? "OR" : "TO");
                eventDetails.add(((Team) t.getOwner()).get(Team.UNIFORM_COLOR));
            } else {
                events.add("OFF");
            }
            bAddTimeToDetails = true;
        }
        if (bAddTimeToDetails) { eventDetails.add(endTime); }
        if (!events.isEmpty()) {
            setCell(row, 3, String.join("; ", events));
            setCell(row, 4, String.join("; ", eventDetails));
        }
    }

    private void setEventInfoCell(Row row, int col, String key) {
        ValWithId kv = game.get(Game.EVENT_INFO, key);
        setCell(row, col, kv == null ? "" : kv.getValue());
    }

    private void setCell(Row row, int col, String value) { setCell(row, col, value, ""); }
    private void setCell(Row row, int col, String value, CellStyle style) {
        setCell(row, col, value, "");
        row.getCell(col).setCellStyle(style);
    }
    private void setCell(Row row, int col, String value, String comment) {
        Cell cell = row.getCell(col);
        if (!"".equals(value)) {
            cell.setCellValue(value);
        } else {
            cell.setBlank();
        }
        setComment(cell, comment);
    }
    private void setCell(Row row, int col, double value) { setCell(row, col, value, ""); }
    private void setCell(Row row, int col, double value, String comment) {
        Cell cell = row.getCell(col);
        cell.setCellValue(value);
        setComment(cell, comment);
    }
    private void setCell(Row row, int col, List<Integer> values, List<String> comments) {
        Cell cell = row.getCell(col);
        if (values.size() > 1) {
            cell.setCellFormula(values.stream().map(String::valueOf).collect(Collectors.joining("+")));
        } else if (values.size() == 1) {
            cell.setCellValue(values.get(0));
        }
        setComment(cell, String.join("; ", comments));
    }

    private static void setComment(Cell cell, String text) {
        if ("".equals(text)) { return; }

        Row row = cell.getRow();
        Sheet sheet = row.getSheet();
        CreationHelper factory = sheet.getWorkbook().getCreationHelper();

        Comment comment = cell.getCellComment();

        if (comment == null) {
            Drawing<?> drawing = sheet.createDrawingPatriarch();

            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(cell.getColumnIndex());
            anchor.setCol2(cell.getColumnIndex() + 2);
            anchor.setRow1(row.getRowNum());
            anchor.setRow2(row.getRowNum() + 3);
            comment = drawing.createCellComment(anchor);
        }

        RichTextString str = factory.createRichTextString(text);
        comment.setString(str);
        comment.setAuthor("CRG");

        cell.setCellComment(comment);
    }

    private void createCellStyles(Sheet igrf) {
        Font strikeFont = wb.createFont();
        strikeFont.setStrikeout(true);
        strikedNum = wb.createCellStyle();
        strikedNum.cloneStyleFrom(igrf.getRow(13).getCell(1).getCellStyle());
        strikedNum.setFont(strikeFont);
        strikedName = wb.createCellStyle();
        strikedName.cloneStyleFrom(igrf.getRow(13).getCell(2).getCellStyle());
        strikedName.setFont(strikeFont);
    }

    private Game game;
    private Workbook wb;
    private CellStyle strikedNum;
    private CellStyle strikedName;
    private Object coreLock;

    // Officials names for filling sheet headers
    private String pt = "";
    private String[][] sk = {{"", ""}, {"", ""}};
    private String[][] jr = {{"", ""}, {"", ""}};
    private String[][] lt = {{"", ""}, {"", ""}};
    private String[][] pbt = {{"", ""}, {"", ""}};

    private List<String> injuries;

    private Boolean hadOsOffset = false;
    private List<String> osOffsetReasons = new ArrayList<>();
}
