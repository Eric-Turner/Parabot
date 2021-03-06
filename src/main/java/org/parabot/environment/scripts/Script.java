package org.parabot.environment.scripts;

import org.parabot.core.Context;
import org.parabot.core.Core;
import org.parabot.core.ui.BotUI;
import org.parabot.core.ui.Logger;
import org.parabot.environment.api.utils.PBPreferences;
import org.parabot.environment.api.utils.Time;
import org.parabot.environment.scripts.framework.*;
import org.parabot.environment.scripts.framework.Frameworks;
import org.parabot.environment.scripts.randoms.Random;

import java.util.Collection;

/**
 *
 * Script template, scripts are 'add-ons' which executes various tasks in-game
 *
 * @author Everel
 *
 */
public class Script implements Runnable {
	public static final int TYPE_STRATEGY = 0;
	public static final int TYPE_LOOP = 1;
	public static final int TYPE_OTHER = 2;

	public static final int STATE_RUNNING = 0;
	public static final int STATE_PAUSE = 1;
	public static final int STATE_STOPPED = 2;

	private Collection<Strategy> strategies;
	private PBPreferences preferences;
	private AbstractFramework frameWork;
	private int state;
	private int frameWorkType;
	private int scriptID;

	public boolean onExecute() {
		return true;
	}

	public void onFinish() {

	}

	public final void provide(final Collection<Strategy> strategies) {
		this.strategies = strategies;
	}

	public final int getFrameWorkType() {
		return frameWorkType;
	}

	public final void setFrameWork(int frameWorkType) {
		if(frameWorkType < 0 || frameWorkType > 2) {
			throw new RuntimeException("Invalid framework type");
		}
		this.frameWorkType = frameWorkType;
	}

	public final void setAbstractFrameWork(AbstractFramework f) {
		this.frameWork = f;
	}

	public final void addRandom(Random random) {
		Context.getInstance().getRandomHandler().addRandom(random);
	}

	@Override
	public final void run() {
		Context context = Context.getInstance();

		Core.verbose("Initializing script...");
		context.getServerProvider().initScript(this);
		Core.verbose("Done.");

		if(!onExecute()) {
			Core.verbose("Script#onExecute returned false, unloading and stopping script...");
			context.getServerProvider().unloadScript(this);
			this.state = STATE_STOPPED;
			Core.verbose("Done.");
			return;
		}

		Core.verbose("Detecting script framework...");
		context.setRunningScript(this);
		BotUI.getInstance().toggleRun();
		if(this instanceof LoopTask) {
			Core.verbose("Script framework detected: LoopTask");
			frameWorkType = TYPE_LOOP;
			frameWork = Frameworks.getLooper((LoopTask) this);
		} else if(strategies != null && !strategies.isEmpty()) {
			Core.verbose("Script framework detected: Strategies");
			frameWorkType = TYPE_STRATEGY;
			frameWork = Frameworks.getStrategyWorker(strategies);
		} else {
			Core.verbose("Unknown script framework: Other");
			frameWorkType = TYPE_OTHER;
		}
		Core.verbose("Running script...");
		Logger.addMessage("Script started.", true);
		try {
			while(this.state != STATE_STOPPED) {
				if(context.getRandomHandler().checkAndRun()) {
					continue;
				}

				if(this.state == STATE_PAUSE) {
					sleep(500);
					continue;
				}
				if(!frameWork.execute()) {
					break;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		Core.verbose("Script stopped/finished, unloading and stopping...");
		onFinish();
		Logger.addMessage("Script stopped.", false);
		context.getServerProvider().unloadScript(this);
		this.state = STATE_STOPPED;
		context.setRunningScript(null);

		if (context.getUlirathaClient() != null) {
			context.getUlirathaClient().disconnect();
			context.setUlirathaClient(null);
		}

		BotUI.getInstance().toggleRun();
		Core.verbose("Done.");
	}

	/**
	 * Sleeps until the SleepCondition is valid.
	 *
	 * <B>DEPRECATED!</b> use {@link Time#sleep(SleepCondition, int)}
	 *
	 * @param conn
	 *            the condition.
	 * @param timeout
	 *            the time in miliseconds before it stops sleeping.
	 * @return whether it ran successfully without timing out.
	 */
	@Deprecated
	public final boolean sleep(SleepCondition conn, int timeout) {
		return Time.sleep(conn, timeout);
	}

	/**
	 * Sets the script's state
	 * @param state
	 */
	public final void setState(final int state) {
		if(state < 0 || state > 2) {
			throw new IllegalArgumentException("Illegal state");
		}
		Core.setBugsnagInformation("Script", "State", String.valueOf(state));
		this.state = state;
	}

	/**
	 * Sleeps for an amount of milliseconds
	 * @param ms
	 */
	public final void sleep(int ms) {
		Time.sleep(ms);
	}

	public int getState() {
		return state;
	}

	public PBPreferences getPreferences(){
		if (this.preferences == null){
			this.preferences = new PBPreferences(scriptID);
		}
		return this.preferences;
	}

	public void setScriptID(int scriptID){
		this.scriptID = scriptID;
		Core.setBugsnagInformation("Script", "State", String.valueOf(scriptID));
	}
}
