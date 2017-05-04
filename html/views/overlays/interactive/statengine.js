/* Stats Library
   Author: Flat Track Protocol

*/


(function( WS ) {

	if(!WS) throw('StatsEngine: Unable to find Scoreboard WS interface');


	var StatsEngine = (function() {

		var S = { }; /* Public Functions */
		var defaults = { debug: true};
		var options = { };
		var StatsFunction = { }; /* Stats Handlers Bits mapped to elements */

		var Data = {};
	 	Data.Summary = { };
	 	Data.Config = { };
		Data.Config.DefaultClocks = { };
		Data.Jams = new DataSet();

		Data.Team = [ { }, { } ];
		Data.Skaters = new DataSet()
		Data.JamSkater = new DataSet();


		function timeString2ms(a,b,c){// time(HH:MM:SS.mss)
		    if(!a) { return }
		    return c=0, a=a.split('.'), !a[1]||(c+=a[1]*1), a=a[0].split(':'),b=a.length, c+=(b==3?a[0]*3600+a[1]*60+a[2]*1:b==2?a[0]*60+a[1]*1:s=a[0]*1)*1e3, c ;
		}

		var _getKeyObject = function(k) {
			var reSK = /^Game.Period\(([0-9])\)\.Jam\((.+)\)\.Team\(([0-9])\)\.Skater\((.+)\)\.(.+)$/;
			m = k.match(reSK);
			if(m) return { Type: 'JamSkater', FullKey: k, 'Period': m[1], Jam: m[2], Team: m[3], Position: m[4], Key: m[5] };

			var reJD = /^Game.Period\(([0-9])\)\.Jam\((.+)\)\.Team\(([0-9])\)\.(.+)$/;
			m = k.match(reJD);	
			if(m) return { Type: 'JamTeam', FullKey: k, Period: m[1], Jam: m[2], Team: m[3], Key: m[4] };

			var reJD = /^Game.Period\(([0-9])\)\.Jam\((.+)\)\.(.+)$/;
			m = k.match(reJD); 
			if(m) return { Type: 'Jam', FullKey: k, Period: m[1], Jam: m[2], Key: m[3] };

			var reSk = /^Game.Team\(([0-9])\)\.Skater\((.+)\)\.(.+)$/;
			m = k.match(reSk); 
			if(m) return { Type: 'Skater', FullKey: k, Skater: m[2], Team: m[1], Key: m[3] }

			var reCF = /^ScoreBoard.Clock\((.+)\)\.(.+)$/;
			m = k.match(reCF);
			if(m) return { Type: 'Setting', FullKey: k, ClockType: m[1], Key: m[2] };

			return false;
		}

		var _handleJamData = function(k,v) {
			kd = _getKeyObject(k);
			console.log('Using Object',kd);
			if(kd) {
				if(StatsFunction[kd.Type] && StatsFunction[kd.Type][kd.Key]) { 
					StatsFunction[kd.Type][kd.Key](kd,v);	
				}
			}
		}

		StatsFunction.Skater = {
			'Name':   function(kd,v) { console.log('Name', kd,v); Data['Skaters'].Upsert( { 'Name': v }, { Team: kd.Team, Skater: kd.Skater } ); },
			'Number': function(kd,v) { console.log('Number', kd,v); Data['Skaters'].Upsert( { 'Number': v }, { Team: kd.Team, Skater: kd.Skater } ); },
			'Flags':  function(kd,v) { console.log('Flags', kd,v); Data['Skaters'].Upsert( { 'Flags': v }, { Team: kd.Team, Skater: kd.Skater } ); }
		}

		StatsFunction.Setting = {
			MaximumTime: function(kd,v) {
				Data.Config.DefaultClocks[kd.ClockType] = v; /* Milliseconds */
				if(kd.ClockType == 'Jam' && v != null) {
					var c = Data['Jams'].All().Each(function() {
						t = parseInt(this.TimeRemaining/1000);
						dc = Data.Config.DefaultClocks.Jam / 1000;
						tv = dc - t;
						mn = Math.floor(tv/60); ms = tv%60;
						tc = (10>mn?'0':'')+mn+':'+(10>ms?'0':'')+ms;
						this.set({ Duration: tv, DurationHuman: tc });
					});
				}
			}
		}

		StatsFunction.JamSkater = { 
			Id: function(kd,v) {
				Data['JamSkater'].Upsert( { Position: kd.Position }, { Period: kd.Period, Jam: kd.Jam, Skater: v} );
			},
		}

		StatsFunction.Jam = {
			Jam: function(kd,v) {
				Data['Jams'].Upsert( { }, { Jam: kd.Jam, Period: kd.Period } );
				Data.Summary.JamCount = Data.Jams.Filter().length;
			},
			JamClock: function(kd,v) {
				t = parseInt(timeString2ms(v));
				dc = Data.Config.DefaultClocks.Jam / 1000;
				tv = dc - (t/1000);
				mn = Math.floor(tv/60);
				ms = Math.floor(tv%60);
				tc = (10>mn?'0':'')+mn+':'+(10>ms?'0':'')+ms;
				console.log(t,dc,tv,mn,ms,tc);
				Data['Jams'].Upsert( { TimeRemaining: t, Duration: tv, DurationHuman: tc }, { Period: kd.Period, Jam: kd.Jam } );
			},
			PeriodClockStart: function(kd,v) {
				Data['Jams'].Upsert( { PeriodClockStart: v }, { Period: kd.Period, Jam: kd.Jam } );
			}
		}

		StatsFunction.JamTeam = { 
			LeadJammer: function(kd,v) {
				if(v == 'NoLead') { return; }

				t = 0;
				if(v == 'Lead') { t = kd.Team }
				if(v == 'LostLead') { t = 0-kd.Team }
			
				jam = Data['Jams'].Upsert( { 'Lead': t }, { Period: kd.Period, Jam: kd.Jam } );

				if(v == 'LostLead') {
					lj = Data['Jams'].Filter( { 'Lead': 0-kd.Team });
					Data.Team[ parseInt(kd.Team) - 1 ]['LeadLostCount'] = lj.length;
					Data.Team[ parseInt(kd.Team) - 1 ]['LeadLostPercent'] = lj.length / Data.Team[kd.Team-1].LeadJammerCount * 100;
				}
				if(v == 'Lead') {
					lj = Data['Jams'].Filter( { 'Lead': kd.Team });
					Data.Team[ parseInt(kd.Team) - 1 ]['LeadJammerCount'] = lj.length;
					Data.Team[ parseInt(kd.Team) - 1 ]['LeadJammerPercent'] = lj.length / Data.Summary.JamCount * 100;
				}

			},	
		}

		S.Initialize = function(opt) {
			$.extend(options, defaults, opt);
			if(options.debug) console.debug('Initialize');

			if(!WS.socket) WS.Connect();
			WS.Register( [ 'ScoreBoard.Clock', 'Game.Period', 'Game.Team' ], function(k,v) { _handleJamData(k,v); } );
			return this;
		}

		S.GetSomething = function() { return Data; }

		return S;

	}());
	window.StatsEngine = StatsEngine;
})(WS);



