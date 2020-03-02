package net.runelite.client.plugins.pktools;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.api.VarClientInt;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.plugins.pktools.ScriptCommand.InputHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

//item id lists at bottom for clarity

@Singleton
public class PkToolsOverlay extends Overlay
{
	private final Client client;
	private final PkToolsPlugin pkToolsPlugin;
	private final SpriteManager spriteManager;
	private final PanelComponent imagePanelComponent = new PanelComponent();

	private static final Color NOT_ACTIVATED_BACKGROUND_COLOR = new Color(150, 0, 0, 150);

	private Dimension panelSize;

	@Getter(AccessLevel.PUBLIC)
	@Setter(AccessLevel.PUBLIC)
	public int specCheck;

	@Setter
	@Getter(AccessLevel.PUBLIC)
	public static Point lastEnemyLocation;

	@Inject
	private PkToolsConfig config;

	@Inject
	private PkToolsOverlay(Client client, PkToolsPlugin pkToolsPlugin, SpriteManager spriteManager)
	{
		this.client = client;
		this.pkToolsPlugin = pkToolsPlugin;
		this.spriteManager = spriteManager;

		this.setPosition(OverlayPosition.BOTTOM_RIGHT);
		this.setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.client.getGameState() != GameState.LOGGED_IN)
			return null;

		Player lastEnemy = this.pkToolsPlugin.lastEnemy;

		ImageComponent PROTECT_MELEE_IMG = new ImageComponent(this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MELEE, 0));
		ImageComponent PROTECT_MISSILES_IMG = new ImageComponent(this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0));
		ImageComponent PROTECT_MAGIC_IMG = new ImageComponent(this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0));

		if (this.panelSize == null)
		{
			this.panelSize = PROTECT_MAGIC_IMG.getBounds().getSize();

			if (this.panelSize.getHeight() != 0 && this.panelSize.getWidth() != 0)
				this.setPreferredSize(this.panelSize);
		}

		this.imagePanelComponent.getChildren().clear();

		Widget SPECBAR = this.client.getWidget(WidgetInfo.COMBAT_SPECIAL_ATTACK);

		if (!SPECBAR.isHidden())
			this.setSpecCheck(1);
		else
			this.setSpecCheck(0);

		Widget INVENTORY_TAB = this.client.isResized() ? this.client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_COMBAT_TAB) : this.client.getWidget(WidgetInfo.FIXED_VIEWPORT_COMBAT_TAB);

		Point COMBAT_POINT = new Point(INVENTORY_TAB.getCanvasLocation().getX(), INVENTORY_TAB.getCanvasLocation().getY());

		int TRIGGERX = (COMBAT_POINT.getX() + 18);
		int TRIGGERY = (COMBAT_POINT.getY() - 8);

		Widget PROTECT_FROM_MELEE = this.client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MELEE);
		Widget PROTECT_FROM_RANGED = this.client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
		Widget PROTECT_FROM_MAGIC = this.client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);

		boolean PROTECT_MELEE = this.client.getVar(Prayer.PROTECT_FROM_MELEE.getVarbit()) != 0;
		boolean PROTECT_RANGED = this.client.getVar(Prayer.PROTECT_FROM_MISSILES.getVarbit()) != 0;
		boolean PROTECT_MAGIC = this.client.getVar(Prayer.PROTECT_FROM_MAGIC.getVarbit()) != 0;

		boolean shouldAutoSwap = this.config.autoPrayerSwitcher() == PkToolsAutoPrayerModes.AUTO || (this.config.autoPrayerSwitcher() == PkToolsAutoPrayerModes.HOTKEY && PkToolsHotkeyListener.prayer_hotkey);
		boolean onPrayerTab = this.client.getVar(VarClientInt.INTERFACE_TAB) == 5;

		if (lastEnemy == null)
		{
			graphics.clearRect(TRIGGERX, TRIGGERY, 4, 4);
			PkToolsOverlay.setLastEnemyLocation(null);

			return null;
		}

		net.runelite.api.Point textLocation = lastEnemy.getCanvasTextLocation(graphics, " ", lastEnemy.getLogicalHeight() / 2);

		if (textLocation != null)
		{
			PkToolsOverlay.setLastEnemyLocation(textLocation);

			graphics.setColor(new Color(0, 255, 245));
			graphics.fillRect(textLocation.getX(), textLocation.getY(), 4, 4);
		}

		if (this.client.getBoostedSkillLevel(Skill.PRAYER) > 0)
		{

			int WEAPON_INT = Objects.requireNonNull(lastEnemy.getPlayerAppearance()).getEquipmentId(KitType.WEAPON);

			if (WEAPON_INT > 0)
			{
				if (Arrays.stream(PkToolsOverlay.MELEE_LIST).anyMatch(x -> x == WEAPON_INT))
				{
					if (this.config.prayerHelper())
					{
						this.imagePanelComponent.getChildren().add(PROTECT_MELEE_IMG);
						this.imagePanelComponent.setBackgroundColor(PROTECT_MELEE ? ComponentConstants.STANDARD_BACKGROUND_COLOR : PkToolsOverlay.NOT_ACTIVATED_BACKGROUND_COLOR);
					}

					if (shouldAutoSwap && !PROTECT_MELEE)
					{
						if (!onPrayerTab)
							InputHandler.sendKey(client.getCanvas(), this.config.prayerTabKey().getKey());

						if (onPrayerTab)
						{
							Point p = InputHandler.getClickPoint(PROTECT_FROM_MELEE.getBounds());
							InputHandler.leftClick(this.client, p);
							InputHandler.sendKey(client.getCanvas(), this.config.inventoryTabKey().getKey());
						}
					}
				}
				else if (Arrays.stream(PkToolsOverlay.RANGED_LIST).anyMatch(x -> x == WEAPON_INT))
				{
					if (this.config.prayerHelper())
					{
						this.imagePanelComponent.getChildren().add(PROTECT_MISSILES_IMG);
						this.imagePanelComponent.setBackgroundColor(PROTECT_RANGED ? ComponentConstants.STANDARD_BACKGROUND_COLOR : PkToolsOverlay.NOT_ACTIVATED_BACKGROUND_COLOR);
					}
					if (shouldAutoSwap && !PROTECT_RANGED)
					{
						if (!onPrayerTab)
							InputHandler.sendKey(client.getCanvas(), this.config.prayerTabKey().getKey());

						if (onPrayerTab)
						{
							Point p = InputHandler.getClickPoint(PROTECT_FROM_RANGED.getBounds());
							InputHandler.leftClick(this.client, p);
							InputHandler.sendKey(client.getCanvas(), this.config.inventoryTabKey().getKey());
						}
					}
				}
				else if (Arrays.stream(PkToolsOverlay.MAGIC_LIST).anyMatch(x -> x == WEAPON_INT))
				{
					if (this.config.prayerHelper())
					{
						this.imagePanelComponent.getChildren().add(PROTECT_MAGIC_IMG);
						this.imagePanelComponent.setBackgroundColor(PROTECT_MAGIC ? ComponentConstants.STANDARD_BACKGROUND_COLOR : PkToolsOverlay.NOT_ACTIVATED_BACKGROUND_COLOR);
					}

					if (shouldAutoSwap && !PROTECT_MAGIC)
					{
						if (!onPrayerTab)
							InputHandler.sendKey(client.getCanvas(), this.config.prayerTabKey().getKey());

						if (onPrayerTab)
						{
							Point p = InputHandler.getClickPoint(PROTECT_FROM_MAGIC.getBounds());
							InputHandler.leftClick(this.client, p);
							InputHandler.sendKey(client.getCanvas(), this.config.inventoryTabKey().getKey());
						}
					}
				}
			}
		}

		return this.imagePanelComponent.render(graphics);
	}

	private static final int[] MELEE_LIST = {ItemID.DRAGON_SCIMITAR, ItemID.RUNE_SCIMITAR, ItemID.GRANITE_MAUL,
			ItemID.ABYSSAL_WHIP, ItemID.DORGESHUUN_CROSSBOW, ItemID.BANDOS_GODSWORD, ItemID.ARMADYL_GODSWORD,
			ItemID.VOLCANIC_ABYSSAL_WHIP, ItemID.FROZEN_ABYSSAL_WHIP, ItemID.BARRELCHEST_ANCHOR,
			ItemID.DRAGON_WARHAMMER, ItemID.ELDER_MAUL, ItemID.ABYSSAL_TENTACLE, ItemID.GHRAZI_RAPIER,
			ItemID.DRAGON_DAGGERP_5698, ItemID.DHAROKS_GREATAXE_100, ItemID.DHAROKS_GREATAXE_75,
			ItemID.DHAROKS_GREATAXE_50, ItemID.DHAROKS_GREATAXE_25, ItemID.DRAGON_CLAWS, ItemID.DRAGON_SCIMITAR_OR,
			ItemID.TZHAARKETOM, ItemID.VERACS_FLAIL_100, ItemID.VERACS_FLAIL_75, ItemID.VERACS_FLAIL_50,
			ItemID.VERACS_FLAIL_25, ItemID.ARCLIGHT, ItemID.WILDERNESS_SWORD_1, ItemID.ABYSSAL_DAGGER_P_13271,
			ItemID.DRAGON_DAGGER, ItemID.DRAGON_DAGGERP, ItemID.DRAGON_DAGGERP_5680, ItemID.DRAGON_DAGGER_20407,
			ItemID.DRAGON_LONGSWORD, ItemID.DRAGON_BATTLEAXE, ItemID.DRAGON_HALBERD, ItemID.DRAGON_2H_SWORD,
			ItemID.DRAGON_SCIMITAR_20406, ItemID.DRAGON_2H_SWORD_20559, ItemID.DRAGON_WARHAMMER_20785,
			ItemID.DRAGON_SWORD, ItemID.DRAGON_SWORD_21206, ItemID.DRAGON_THROWNAXE_21207,
			ItemID.BARRELCHEST_ANCHOR_10888, ItemID.GRANITE_MAUL_12848, ItemID.GRANITE_MAUL_20557,
			ItemID.ELDER_MAUL_21205, ItemID.TOKTZXILAK, ItemID.TOKTZXILEK, ItemID.TOKTZXILAK_20554,
			ItemID.LEAFBLADED_BATTLEAXE, ItemID.LEAFBLADED_SPEAR, ItemID.LEAFBLADED_SPEAR_4159, ItemID.LEAFBLADED_SWORD,
			ItemID.ZAMORAK_GODSWORD, ItemID.ZAMORAK_GODSWORD_OR, ItemID.SARADOMIN_GODSWORD, ItemID.SARADOMIN_GODSWORD_OR,
			ItemID.ARMADYL_GODSWORD_OR, ItemID.ARMADYL_GODSWORD_20593, ItemID.ARMADYL_GODSWORD_22665,
			ItemID.BANDOS_GODSWORD_20782, ItemID.BANDOS_GODSWORD_21060, ItemID.BANDOS_GODSWORD_OR,
	};

	private static final int[] RANGED_LIST = {ItemID.RUNE_CROSSBOW, ItemID.MAGIC_SHORTBOW_I, ItemID.ARMADYL_CROSSBOW,
			ItemID.TOXIC_BLOWPIPE, ItemID.DARK_BOW, ItemID.MAPLE_SHORTBOW, ItemID.LIGHT_BALLISTA, ItemID.HEAVY_BALLISTA,
			ItemID.MAGIC_SHORTBOW, ItemID.MAGIC_SHORTBOW_20558, ItemID.DRAGON_THROWNAXE, ItemID.DRAGON_HUNTER_CROSSBOW,
			ItemID.DRAGON_THROWNAXE_21207, ItemID.TOKTZXILUL, ItemID.NEW_CRYSTAL_BOW, ItemID.CRYSTAL_BOW_FULL,
			ItemID.CRYSTAL_BOW_910, ItemID.CRYSTAL_BOW_810, ItemID.CRYSTAL_BOW_710, ItemID.CRYSTAL_BOW_610,
			ItemID.CRYSTAL_BOW_510, ItemID.CRYSTAL_BOW_410, ItemID.CRYSTAL_BOW_310, ItemID.CRYSTAL_BOW_210,
			ItemID.CRYSTAL_BOW_110, ItemID.KARILS_CROSSBOW, ItemID.KARILS_CROSSBOW_100, ItemID.KARILS_CROSSBOW_75,
			ItemID.KARILS_CROSSBOW_50, ItemID.KARILS_CROSSBOW_25, ItemID.KARILS_CROSSBOW_0, ItemID.BRONZE_CROSSBOW,
			ItemID.BLURITE_CROSSBOW, ItemID.IRON_CROSSBOW, ItemID.STEEL_CROSSBOW, ItemID.MITH_CROSSBOW,
			ItemID.ADAMANT_CROSSBOW, ItemID.HUNTERS_CROSSBOW, ItemID._3RD_AGE_BOW, ItemID.DARK_BOW_12765,
			ItemID.DARK_BOW_12766, ItemID.DARK_BOW_12767, ItemID.DARK_BOW_12768, ItemID.DARK_BOW_20408,
			ItemID.TWISTED_BOW, ItemID.DRAGON_CROSSBOW
	};

	private static final int[] MAGIC_LIST = {ItemID.TOXIC_STAFF_OF_THE_DEAD, ItemID.MYSTIC_SMOKE_STAFF,
			ItemID.CLUE_SCROLL_MEDIUM_19772, ItemID.IBANS_STAFF_U, ItemID.ANCIENT_STAFF, ItemID.STAFF_OF_THE_DEAD,
			ItemID.TRIDENT_OF_THE_SEAS_FULL, ItemID.TRIDENT_OF_THE_SEAS, ItemID.STAFF_OF_AIR, ItemID.MYSTIC_DUST_STAFF,
			ItemID.STAFF_OF_FIRE, ItemID.TOXIC_STAFF_UNCHARGED, ItemID.SARADOMIN_STAFF, ItemID.MYSTIC_WATER_STAFF,
			ItemID.STAFF_OF_LIGHT, ItemID.STAFF_OF_WATER, ItemID.WATER_BATTLESTAFF, ItemID.AHRIMS_STAFF,
			ItemID.STAFF_OF_EARTH, ItemID.MAGIC_STAFF, ItemID.BATTLESTAFF, ItemID.FIRE_BATTLESTAFF,
			ItemID.AIR_BATTLESTAFF, ItemID.EARTH_BATTLESTAFF, ItemID.MYSTIC_FIRE_STAFF, ItemID.MYSTIC_AIR_STAFF,
			ItemID.MYSTIC_EARTH_STAFF, ItemID.IBANS_STAFF, ItemID.GUTHIX_STAFF, ItemID.ZAMORAK_STAFF,
			ItemID.LAVA_BATTLESTAFF, ItemID.MYSTIC_LAVA_STAFF, ItemID.AHRIMS_STAFF_100, ItemID.AHRIMS_STAFF_75,
			ItemID.AHRIMS_STAFF_50, ItemID.AHRIMS_STAFF_25, ItemID.AHRIMS_STAFF_0, ItemID.MUD_BATTLESTAFF,
			ItemID.MYSTIC_MUD_STAFF, ItemID.WHITE_MAGIC_STAFF, ItemID.LUNAR_STAFF, ItemID.SMOKE_BATTLESTAFF,
			ItemID.STEAM_BATTLESTAFF_12795, ItemID.MYSTIC_STEAM_STAFF_12796, ItemID.ANCIENT_STAFF_20431,
			ItemID.MIST_BATTLESTAFF, ItemID.MYSTIC_MIST_STAFF, ItemID.DUST_BATTLESTAFF, ItemID.LAVA_BATTLESTAFF_21198,
			ItemID.MYSTIC_LAVA_STAFF_21200, ItemID.BEGINNER_WAND, ItemID.APPRENTICE_WAND, ItemID.TEACHER_WAND,
			ItemID.MASTER_WAND, ItemID.WAND, ItemID.INFUSED_WAND, ItemID._3RD_AGE_WAND, ItemID.BEGINNER_WAND_20553,
			ItemID.APPRENTICE_WAND_20556, ItemID.MASTER_WAND_20560, ItemID.KODAI_WAND, ItemID.TOKTZMEJTAL
	};
}