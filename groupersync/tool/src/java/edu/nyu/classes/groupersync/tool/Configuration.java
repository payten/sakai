package edu.nyu.classes.groupersync.tool;

import org.sakaiproject.component.cover.ServerConfigurationService;

class Configuration {

    public static String getSkinRepo() {
        return ServerConfigurationService.getString("skin.repo", "");
    }

    public static String getPortalUrl() {
        return ServerConfigurationService.getPortalUrl();
    }

    public static int getMaxDescriptionLength() {
        return 70;
    }

    public static int getMaxAddressLength() {
        return 60;
    }

    public static String getDescriptionExcludedCharacters() {
        return ":";
    }

    public static String getAddressAllowedCharacters() {
        return "a-zA-Z0-9_.";
    }

    public static String getWhitespaceReplacementCharacter() {
        return "-";
    }

}
