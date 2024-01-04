package net.angercommand;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Initialization implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("anger-command");

	@Override
	public void onInitialize() {
		AngerCommand.register();
		LOGGER.info("The anger command was successfully loaded!");
	}
}