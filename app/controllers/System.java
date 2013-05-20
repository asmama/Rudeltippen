package controllers;

import interfaces.AppConstants;

import java.util.Date;
import java.util.List;

import models.Game;
import models.Settings;
import models.User;
import play.Logger;
import play.Play;
import play.db.jpa.NoTransaction;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Before;
import play.mvc.Controller;
import play.test.Fixtures;
import services.AppService;
import utils.SetupUtils;
import utils.ValidationUtils;

public class System extends Controller implements AppConstants {
	
	@Before()
	protected static void before() {
		AppService.setAppLanguage();
	}
	public static void setup() {
		if (AppService.rudeltippenIsInizialized()) {
			redirect("/");
		}
		render();
	}
	
	public static void init() {
		if (!AppService.rudeltippenIsInizialized()) {
			session.clear();
			response.removeCookie("rememberme");

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Logger.error("Failed while trying to sleep in system/init", e);
			}

			Fixtures.deleteAllModels();
			Fixtures.deleteDatabase();
			Fixtures.loadModels(YAMLFILE);
			
			final List<Game> prePlayoffGames = Game.find("byPlayoff", false).fetch();
			final List<Game> playoffGames = Game.find("byPlayoff", true).fetch();
			boolean hasPlayoffs = false;
			if ((playoffGames != null) && (playoffGames.size() > 0)) {
				hasPlayoffs = true;
			}

			Settings settings = AppService.getSettings();
			settings = AppService.getSettings();
			settings.setAppSalt(Codec.hexSHA1(Codec.UUID()));
			settings.setGameName("Rudeltippen");
			settings.setPointsTip(4);
			settings.setPointsTipDiff(2);
			settings.setPointsTipTrend(1);
			settings.setMinutesBeforeTip(5);
			settings.setPlayoffs(hasPlayoffs);
			settings.setNumPrePlayoffGames(prePlayoffGames.size());
			settings.setInformOnNewTipper(true);
			settings.setEnableRegistration(true);
			settings._save();

			User user = new User();
			final String salt = Codec.hexSHA1(Codec.UUID());
			user.setSalt(salt);
			user.setEmail("admin@foo.bar");
			user.setUsername("admin");
			user.setUserpass(AppService.hashPassword("admin", salt));
			user.setRegistered(new Date());
			user.setExtraPoints(0);
			user.setTipPoints(0);
			user.setPoints(0);
			user.setActive(true);
			user.setAdmin(true);
			user.setReminder(true);
			user.setCorrectResults(0);
			user.setCorrectDifferences(0);
			user.setCorrectTrends(0);
			user.setCorrectExtraTips(0);
			user._save();
			ok();
		}
		redirect("/");
	}

	@NoTransaction
	public static void yamler() {
		if (("true").equals(Play.configuration.getProperty("yamler"))) {
			final List<String> playdays = SetupUtils.generatePlaydays(34);
			final List<String> games = SetupUtils.getGamesFromWebService(34, "bl1", "2012");
			render(playdays, games);
		}
		notFound();
	}
}