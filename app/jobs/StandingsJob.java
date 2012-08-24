package jobs;

import java.util.List;

import models.Game;
import models.User;
import play.Logger;
import play.i18n.Messages;
import play.jobs.Job;
import play.jobs.On;
import services.MailService;
import services.TwitterService;
import utils.AppUtils;

@On("0 0 3 * * ?")
public class StandingsJob extends Job{
	@Override
	public void doJob() {
		if (AppUtils.isJobInstance()) {
			Logger.info("Running job: StandingsJob");

			String message = "";
			final Game game = Game.find("byNumber", 1).first();
			if ((game != null) && game.isEnded()) {
				int count = 1;
				final StringBuilder buffer = new StringBuilder();

				List<User> users = User.find("ORDER BY place ASC").fetch(3);
				for (final User user : users) {
					if (count < 3) {
						buffer.append(user.getNickname() + " (" + user.getPoints() + " " + Messages.get("points") + "), ");
					} else {
						buffer.append(user.getNickname() + " (" + user.getPoints() + " " + Messages.get("points") + ")");
					}
					count++;
				}
				message = Messages.get("topthree") + ": " + buffer.toString();

				if (AppUtils.isTweetable()) {
					TwitterService.updateStatus(message);
				}

				users = User.find("bySendStandings", true).fetch();
				for (final User user : users) {
					MailService.sendStandings(message, user.getUsername());
				}
			}
		}
	}
}