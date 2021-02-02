package ro.dlri.oxygen.plugin;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

/**
 * Workspace access plugin.
 */
public class DlriOxygenPlugin extends Plugin {
	/**
	 * The static plugin instance.
	 */
	private static DlriOxygenPlugin instance = null;

	/**
	 * Constructs the plugin.
	 * 
	 * @param descriptor
	 *            The plugin descriptor
	 */
	public DlriOxygenPlugin(PluginDescriptor descriptor) {
		super(descriptor);

		if (instance != null) {
			throw new IllegalStateException("Already instantiated!");
		}
		instance = this;
	}

	/**
	 * Get the plugin instance.
	 * 
	 * @return the shared plugin instance.
	 */
	public static DlriOxygenPlugin getInstance() {
		return instance;
	}
}
