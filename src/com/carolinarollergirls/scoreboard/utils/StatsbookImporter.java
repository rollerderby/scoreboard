package com.carolinarollergirls.scoreboard.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.carolinarollergirls.scoreboard.core.game.GameImpl;
import com.carolinarollergirls.scoreboard.core.interfaces.Game;
import com.carolinarollergirls.scoreboard.core.interfaces.Official;
import com.carolinarollergirls.scoreboard.core.interfaces.ScoreBoard;
import com.carolinarollergirls.scoreboard.core.interfaces.Skater;
import com.carolinarollergirls.scoreboard.core.interfaces.Team;
import com.carolinarollergirls.scoreboard.event.Child;
import com.carolinarollergirls.scoreboard.event.ScoreBoardEventProviderImpl;

public class StatsbookImporter {
    public StatsbookImporter(ScoreBoard sb) { scoreboard = sb; }

    public void read(InputStream in) {
        try {
            wb = WorkbookFactory.create(in);
            game = new GameImpl(scoreboard, UUID.randomUUID().toString());
            readIgrf();
            scoreboard.runInBatch(new Runnable() {
                @Override
                public void run() {
                    synchronized (coreLock) { scoreboard.add(ScoreBoard.GAME, game); }
                }
            });
        } catch (IOException e) { Logger.printStackTrace(e); }
    }

    private void readIgrf() {
        Sheet igrf = wb.getSheet("IGRF");
        readIgrfHead(igrf);
        readTeam(igrf, Team.ID_1);
        readTeam(igrf, Team.ID_2);
        readOfficials(igrf);
        readExpulsionSuspensionInfo(igrf);
    }

    private void readIgrfHead(Sheet igrf) {
        Row row = igrf.getRow(2);
        readEventInfoCell(row, 1, Game.INFO_VENUE);
        readEventInfoCell(row, 8, Game.INFO_CITY);
        readEventInfoCell(row, 10, Game.INFO_STATE);
        readEventInfoCell(row, 11, Game.INFO_GAME_NUMBER);
        row = igrf.getRow(4);
        readEventInfoCell(row, 1, Game.INFO_TOURNAMENT);
        readEventInfoCell(row, 8, Game.INFO_HOST);
        row = igrf.getRow(6);
        try {
            String dateString = readCell(row, 1);
            // fail on malformed dates
            dateString =
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ISO_LOCAL_DATE);
            game.add(Game.EVENT_INFO, new ValWithId(Game.INFO_DATE, dateString));
        } catch (DateTimeParseException e) {}
        try {
            String timeString = readCell(row, 8);
            // convert from format used in statsbook to HH:mm
            timeString = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("h:mm a")).toString();
            game.add(Game.EVENT_INFO, new ValWithId(Game.INFO_START_TIME, timeString));
        } catch (DateTimeParseException e) {}
    }

    private void readTeam(Sheet igrf, String teamId) {
        Team team = game.getTeam(teamId);
        int col = Team.ID_1.equals(teamId) ? 1 : 8;
        team.set(Team.LEAGUE_NAME, readCell(igrf.getRow(9), col));
        team.set(Team.TEAM_NAME, readCell(igrf.getRow(10), col));
        team.set(Team.UNIFORM_COLOR, readCell(igrf.getRow(11), col));
        String captainName = readCell(igrf.getRow(48), col);

        for (int i = 13; i < 33; ++i) { readSkater(igrf.getRow(i), team, captainName); }
    }

    private void readSkater(Row row, Team team, String captainName) {
        int col = Team.ID_1.equals(team.getProviderId()) ? 1 : 8;
        String number = readCell(row, col);
        String name = readCell(row, col + 1);
        if ("".equals(number) && "".equals(name)) { return; }
        Skater s = team.getOrCreate(Team.SKATER, UUID.randomUUID().toString());
        if (number.endsWith("*")) {
            s.setFlags("ALT");
            number = number.substring(0, number.length() - 1);
        }
        s.setRosterNumber(number);
        s.setName(name);
    }

    private void readOfficials(Sheet igrf) {
        Child<Official> type = Game.NSO;
        for (int i = 59; i < 88; ++i) { type = readOfficial(igrf.getRow(i), type); }
    }

    private Child<Official> readOfficial(Row row, Child<Official> lastType) {
        String role = readCell(row, 0);
        if ("".equals(role)) { return Game.REF; }
        Child<Official> type;
        switch (role) {
        case Official.ROLE_HR:
        case Official.ROLE_IPR:
        case Official.ROLE_JR:
        case Official.ROLE_OPR:
        case Official.ROLE_ALTR: type = Game.REF; break;
        case Official.ROLE_HNSO:
        case Official.ROLE_JT:
        case Official.ROLE_PLT:
        case Official.ROLE_PT:
        case Official.ROLE_WB:
        case Official.ROLE_PW:
        case Official.ROLE_SBO:
        case Official.ROLE_SK:
        case Official.ROLE_PBM:
        case Official.ROLE_PBT:
        case Official.ROLE_LT: type = Game.NSO; break;
        default: type = lastType;
        }
        String name = readCell(row, 2);
        if ("".equals(name)) { return type; }
        String league = readCell(row, 7);
        String cert = readCell(row, 10);
        if (cert.endsWith("1")) {
            cert = "1";
        } else if (cert.endsWith("2")) {
            cert = "2";
        } else if (cert.endsWith("3")) {
            cert = "3";
        } else if (cert.startsWith("R")) {
            cert = "R";
        }
        Official newO = null;
        if (type == Game.NSO) {
            for (Official o : game.getAll(type)) {
                if (o.get(Official.NAME).equals(name) && o.get(Official.LEAGUE).equals(league)) {
                    if (Official.ROLE_HNSO.equals(role)) {
                        newO = o;
                        break;
                    }
                    if (Official.ROLE_HNSO.equals(o.get(Official.ROLE))) {
                        newO = o;
                        o.set(Official.ROLE, role);
                        break;
                    }
                    if (Official.ROLE_PT.equals(role) && Official.ROLE_LT.equals(o.get(Official.ROLE)) ||
                        Official.ROLE_LT.equals(role) && Official.ROLE_PT.equals(o.get(Official.ROLE))) {
                        newO = o;
                        o.set(Official.ROLE, Official.ROLE_PLT);
                        break;
                    }
                }
            }
        }
        if (newO == null) {
            newO = game.getOrCreate(type, UUID.randomUUID().toString());
            newO.set(Official.ROLE, role);
            newO.set(Official.NAME, name);
            newO.set(Official.LEAGUE, league);
            newO.set(Official.CERT, cert);
        }
        if (Official.ROLE_HNSO.equals(role)) { game.set(Game.HEAD_NSO, newO); }
        return type;
    }

    private void readExpulsionSuspensionInfo(Sheet igrf) {
        game.set(Game.SUSPENSIONS_SERVED, readCell(igrf.getRow(39), 4));
    }

    private void readEventInfoCell(Row row, int col, String key) {
        game.add(Game.EVENT_INFO, new ValWithId(key, readCell(row, col)));
    }

    private String readCell(Row row, int col) {
        Cell cell = row.getCell(col);
        return formatter.formatCellValue(cell);
    }

    ScoreBoard scoreboard;
    Game game;
    Workbook wb;
    DataFormatter formatter = new DataFormatter();

    Object coreLock = ScoreBoardEventProviderImpl.getCoreLock();
}
