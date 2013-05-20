package jobs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import models.Game;
import models.Playday;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import play.Logger;
import play.jobs.On;
import services.AppService;
import services.UpdateService;
import utils.SetupUtils;

@On("0 0 5 * * ?")
public class PlaydayJob extends AppJob{

    public PlaydayJob() {
        this.setDescription("Updates the Kickoff time and MatchID of the current and the next three Playdays from OpenLiga.de");
        this.setExecuted("Runs daily at 05:00");
    }

    @Override
    public void doJob() {
        if (AppService.isJobInstance()) {
            Logger.info("Started Job: PlaydayJob");
            int number = AppService.getCurrentPlayday().getNumber();
            for (int i=0; i <= 3; i++) {
                final Playday playday = Playday.find("byNumber", number).first();
                if (playday != null) {
                    final List<Game> games = playday.getGames();
                    for (final Game game : games) {
                        final String matchID = game.getWebserviceID();
                        if (StringUtils.isNotBlank(matchID)) {
                            final Document document = UpdateService.getDocumentFromWebService(matchID);
                            final Date kickoff = SetupUtils.getKickoffFromDocument(document);
                            final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
                            df.setTimeZone(TimeZone.getTimeZone(AppService.getCurrentTimeZone()));

                            game.setKickoff(kickoff);
                            game._save();

                            Logger.info("Updated Kickoff and MatchID of Playday: " + playday.getName());
                        }
                    }
                }
                number++;
            }
            Logger.info("Finished Job: PlaydayJob");
        }
    }
}