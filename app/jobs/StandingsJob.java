package jobs;

import java.util.List;

import models.AbstractJob;
import models.Game;
import models.User;
import play.Logger;
import play.i18n.Messages;
import play.jobs.On;
import utils.AppUtils;
import utils.MailUtils;

@On("0 0 3 * * ?")
public class StandingsJob extends AppJob {

    public StandingsJob() {
        this.setDescription("Sends the current Top 3 to every user who has this notification activated.");
        this.setExecuted("Runs daily at 03:00");
    }

    @Override
    public void doJob() {
        if (AppUtils.isJobInstance()) {
        	AbstractJob job = AbstractJob.find("byName", "StandingsJob").first();
        	if (job != null && job.isActive()) {
                Logger.info("Started Job: StandingsJob");

                String message = "";
                final Game game = Game.find("byNumber", 1).first();
                if ((game != null) && game.isEnded()) {
                    int count = 1;
                    final StringBuilder buffer = new StringBuilder();

                    List<User> users = User.find("SELECT u FROM User u WHERE active = true ORDER BY place ASC").fetch(3);
                    for (final User user : users) {
                        if (count < 3) {
                            buffer.append(user.getUsername() + " (" + user.getPoints() + " " + Messages.get("points") + "), ");
                        } else {
                            buffer.append(user.getUsername() + " (" + user.getPoints() + " " + Messages.get("points") + ")");
                        }
                        count++;
                    }
                    message = Messages.get("topthree") + ": " + buffer.toString();

                    users = User.find("bySendStandings", true).fetch();
                    for (final User user : users) {
                        MailUtils.notifications(Messages.get("mails.top3.subject"), message, user);
                    }
                }
                Logger.info("Finished Job: StandingsJob");
        	}
        }
    }
}