package jobs;

import java.util.ArrayList;
import java.util.List;

import models.Extra;
import models.ExtraTip;
import models.Game;
import models.GameTip;
import models.User;
import play.Logger;
import play.jobs.Job;
import play.jobs.On;
import services.MailService;
import utils.AppUtils;

@On("0 0 1 * * ?")
public class ReminderJob extends Job {
	@Override
	public void doJob() {
		if (AppUtils.isJobInstance()) {
		    Logger.info("Running Job: Reminder");
			final List<Extra> nextExtras = Extra.find("SELECT b FROM Extra b WHERE DATE(ending) = DATE(NOW())").fetch();
			final List<Game> nextGames = Game.find("SELECT g FROM Game g WHERE DATE(kickoff) = DATE(NOW())").fetch();
			final List<User> users = User.find("byReminderAndIsActive", true, true).fetch();
			
			for (User user : users) {
				List<Game> reminderGames = new ArrayList();
				List<Extra> reminderBonus = new ArrayList();
				
				for (Game game : nextGames) {
					final GameTip gameTip = GameTip.find("byGameAndUser", game, user).first();
					if (gameTip == null) {
						reminderGames.add(game);
					}
				}
				
				for (Extra extra : nextExtras) {
					final ExtraTip extraTip = ExtraTip.find("byBonusAndUser", extra, user).first();
					if (extraTip == null) {
						reminderBonus.add(extra);
					}
				}
				
				if (reminderGames.size() > 0 || reminderBonus.size() > 0) {
					MailService.reminder(user, reminderGames, reminderBonus);
					Logger.info("Reminder send to: " + user.getUsername());
				}
			}
		}
	}
}