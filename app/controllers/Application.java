package controllers;

import java.util.List;

import models.Playday;
import models.Settings;
import models.User;
import play.db.jpa.Transactional;
import play.mvc.With;
import utils.AppUtils;

@With(Auth.class)
@Transactional(readOnly=true)
public class Application extends Root {
	public static void index() {
		int pointsDiff = AppUtils.getPointsToFirstPlace();
		final String diffToTop = AppUtils.getDiffToTop(pointsDiff);
		final Playday playday = AppUtils.getCurrentPlayday();
		final List<User> topUsers = User.find("ORDER BY place ASC").fetch(3);
		final long users = User.count();

		render(topUsers, playday, users, diffToTop);
	}

	public static void rules() {
		final Settings settings = AppUtils.getSettings();
		render(settings);
	}
}