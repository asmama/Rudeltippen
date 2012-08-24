package controllers;

import interfaces.AppConstants;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Confirmation;
import models.ConfirmationType;
import models.Extra;
import models.ExtraTip;
import models.Game;
import models.GameTip;
import models.Settings;
import models.User;
import play.Logger;
import play.data.validation.Validation;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.libs.Codec;
import play.libs.Crypto;
import play.libs.Images;
import play.mvc.With;
import services.MailService;
import utils.AppUtils;
import utils.ValidationUtils;

@With(Auth.class)
public class Users extends Root implements AppConstants{
	@Transactional(readOnly=true)
	public static void show(final String nickname) {
		final User user = User.find("byNickname", nickname).first();

		if (user != null) {
			final Map<String, Integer> statistics = new HashMap<String, Integer>();

			final long extra = Extra.count();
			final List<ExtraTip> correctExtraTips = ExtraTip.find("SELECT e FROM ExtraTip e WHERE user = ? AND points > 0", user).fetch();

			statistics.put("extraTips", (int) extra);
			statistics.put("correctExtraTips", correctExtraTips.size());

			final List<GameTip> tips = GameTip.find("byUser", user).fetch();
			final int sumAllTipps = tips.size();
			final int correctTipps = user.getCorrectResults();
			final int correctTrend = user.getCorrectTrends();
			final int correctDifference = user.getCorrectDifferences();
			final int sumTipps = correctTipps + correctTrend + correctDifference;

			statistics.put("sumGames", (int) Game.count());
			statistics.put("sumTipps", sumAllTipps);
			statistics.put("correctTipps", correctTipps);
			statistics.put("correctTrend", correctTrend);
			statistics.put("correctDifference", correctDifference);
			final float pointsTipp = (float) user.getPoints() / (float) sumTipps;
			String pointsPerTipp = "0";
			if (pointsTipp > 0) {
				final DecimalFormat df = new DecimalFormat( "0.00" );
				pointsPerTipp = df.format( pointsTipp );
			}

			if (sumTipps > 0) {
				statistics.put("tippQuote", (100 / sumTipps) * correctTipps);
			} else {
				statistics.put("tippQuote", 0);
			}

			render(user, statistics, pointsPerTipp);
		} else {
			redirect("/");
		}
	}

	@Transactional(readOnly=true)
	public static void profile() {
		final User user = AppUtils.getConnectedUser();
		final Settings settings = AppUtils.getSettings();
		render(user, settings);
	}

	public static void updatenickname(final String nickname) {
		if (AppUtils.verifyAuthenticity()) { checkAuthenticity(); }

		validation.required(nickname);
		validation.minSize(nickname, 3);
		validation.maxSize(nickname, 20);
		validation.isTrue(!ValidationUtils.nicknameExists(nickname)).key("nickname").message(Messages.get("controller.users.nicknamexists"));

		if (validation.hasErrors()) {
			params.flash();
			validation.keep();
		} else {
			final User user = AppUtils.getConnectedUser();
			user.setNickname(nickname);
			user._save();

			flash.put("infomessage", Messages.get("controller.profile.updatenickname"));
			Logger.info("Nickname updated: " + user.getUsername() + " / " + nickname);
		}
		flash.keep();

		redirect("/users/profile");
	}

	public static void updateusername(final String username, final String usernameConfirmation) {
		if (AppUtils.verifyAuthenticity()) { checkAuthenticity(); }

		validation.required(username);
		validation.email(username);
		validation.equals(username, usernameConfirmation);
		validation.equals(ValidationUtils.usernameExists(username), false).key("username").message(Messages.get("controller.users.emailexists"));

		if (validation.hasErrors()) {
			params.flash();
			validation.keep();
		} else {
			final String token = Codec.UUID();
			final User user = AppUtils.getConnectedUser();
			if (user != null) {
				final ConfirmationType confirmationType = ConfirmationType.CHANGEUSERNAME;
				final Confirmation confirmation = new Confirmation();
				confirmation.setConfirmType(confirmationType);
				confirmation.setConfirmValue(Crypto.encryptAES(username));
				confirmation.setCreated(new Date());
				confirmation.setToken(token);
				confirmation.setUser(user);
				confirmation._save();
				MailService.confirm(user, token, confirmationType);
				flash.put("infomessage", Messages.get("confirm.message"));
			}
		}
		flash.keep();

		redirect("/users/profile");
	}

	public static void updatepassword(final String userpass, final String userpassConfirmation) {
		if (AppUtils.verifyAuthenticity()) { checkAuthenticity(); }

		validation.required(userpass);
		validation.equals(userpass, userpassConfirmation);
		validation.minSize(userpass, 6);
		validation.maxSize(userpass, 30);

		if (Validation.hasErrors()) {
			params.flash();
			validation.keep();
		} else {
			final String token = Codec.UUID();
			final User user = AppUtils.getConnectedUser();
			if (user != null) {
				final ConfirmationType confirmationType = ConfirmationType.CHANGEUSERPASS;
				final Confirmation confirm = new Confirmation();
				confirm.setConfirmType(confirmationType);
				confirm.setConfirmValue(Crypto.encryptAES(AppUtils.hashPassword(userpass, user.getSalt())));
				confirm.setCreated(new Date());
				confirm.setToken(token);
				confirm.setUser(user);
				confirm._save();
				MailService.confirm(user, token, confirmationType);
				flash.put("infomessage", Messages.get("confirm.message"));
				Logger.info("Password updated: " + user.getUsername());
			}
		}
		flash.keep();

		redirect("/users/profile");
	}

	public static void updatenotifications(final boolean reminder, final boolean notification, final boolean sendstandings) {
		if (AppUtils.verifyAuthenticity()) { checkAuthenticity(); }

		final User user = AppUtils.getConnectedUser();
		user.setReminder(reminder);
		user.setNotification(notification);
		user.setSendStandings(sendstandings);
		user._save();

		flash.put("infomessage", Messages.get("controller.profile.notifications"));
		flash.keep();
		Logger.info("Notifications updated: " + user.getUsername());

		redirect("/users/profile");
	}

	public static void updatepicture(final File picture) {
		if (AppUtils.verifyAuthenticity()) { checkAuthenticity(); }

		validation.required(picture);

		if (picture != null) {
			final int size = AppUtils.getSettings().getMaxPictureSize();
			final String message = Messages.get("profile.maxpicturesize", size / 1024);
			validation.isTrue(ValidationUtils.checkFileLength(picture.length())).key("picture").message(message);
		} else {
			validation.isTrue(false);
		}

		if (validation.hasErrors()) {
			params.flash();
			validation.keep();
		} else {
			final User user = AppUtils.getConnectedUser();
			try {
				Images.resize(picture, picture, PICTURELARGE, PICTURELARGE);
				user.setPictureLarge(Images.toBase64(picture));
				Images.resize(picture, picture, PICTURESMALL, PICTURESMALL);
				user.setPicture(Images.toBase64(picture));
				if (picture.delete()) {
					Logger.warn("User-Picutre could not be deleted after upload.");
				} else {
					Logger.info("User-Picture deleted after upload.");
				}

				user._save();
				flash.put("infomessage", Messages.get("controller.profile.updatepicture"));
				Logger.info("Picture updated: " + user.getUsername());
			} catch (final IOException e) {
				flash.put("warningmessage", Messages.get("controller.profile.updatepicturefail"));
				Logger.error("Failed to save user picture", e);
			}
		}

		flash.keep();
		redirect("/users/profile");
	}

	public static void deletepicture() {
		final User user = AppUtils.getConnectedUser();
		user.setPicture(null);
		user.setPictureLarge(null);
		user._save();

		flash.put("infomessage", Messages.get("controller.profile.deletedpicture"));
		flash.keep();

		redirect("/users/profile");
	}
}