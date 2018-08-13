/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2018 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextMultiline;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndDocument;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptNoLighting;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;

import java.io.IOException;
import java.util.ArrayList;

public class AlchemyScene extends PixelScene {
	
	private static ItemButton[] inputs = new ItemButton[3];
	private ItemSlot output;
	
	private Emitter smokeEmitter;
	private Emitter bubbleEmitter;
	
	private Emitter lowerBubbles;
	private SkinnedBlock water;
	
	private RedButton btnCombine;
	
	private static final int BTN_SIZE	= 28;
	
	@Override
	public void create() {
		super.create();
		
		water = new SkinnedBlock(
				Camera.main.width, Camera.main.height,
				Dungeon.level.waterTex() ){
			
			@Override
			protected NoosaScript script() {
				return NoosaScriptNoLighting.get();
			}
			
			@Override
			public void draw() {
				//water has no alpha component, this improves performance
				Blending.disable();
				super.draw();
				Blending.enable();
			}
		};
		add(water);
		
		Image im = new Image(TextureCache.createGradient(0x66000000, 0x88000000, 0xAA000000, 0xCC000000, 0xFF000000));
		im.angle = 90;
		im.x = Camera.main.width;
		im.scale.x = Camera.main.height/5f;
		im.scale.y = Camera.main.width;
		add(im);
		
		int w = 50 + Camera.main.width/2;
		int left = (Camera.main.width - w)/2;
		
		int pos = (Camera.main.height - 160)/2;
		
		IconTitle titlebar = new IconTitle();
		//titlebar.icon(DungeonTerrainTilemap.tile(0, Terrain.ALCHEMY));
		titlebar.label( Messages.get(AlchemyScene.class, "title") );
		titlebar.setRect( 0, 0, w, 0 );
		//add( titlebar );
		
		pos += titlebar.height() + 2;
		
		RenderedTextMultiline desc = PixelScene.renderMultiline(6);
		desc.text( Messages.get(AlchemyScene.class, "text") );
		desc.maxWidth(w);
		desc.setPos(left + (w - desc.width())/2, pos);
		add(desc);
		
		pos += desc.height() + 6;
		
		synchronized (inputs) {
			for (int i = 0; i < inputs.length; i++) {
				inputs[i] = new ItemButton() {
					@Override
					protected void onClick() {
						super.onClick();
						if (item != null) {
							if (!item.collect()) {
								Dungeon.level.drop(item, Dungeon.hero.pos);
							}
							item = null;
							slot.item(new WndBag.Placeholder(ItemSpriteSheet.SOMETHING));
							updateState();
						}
						AlchemyScene.this.addToFront(WndBag.lastBag( itemSelector, WndBag.Mode.ALCHEMY, Messages.get(AlchemyScene.class, "select")));
					}
				};
				inputs[i].setRect(left + 10, pos, BTN_SIZE, BTN_SIZE);
				add(inputs[i]);
				pos += BTN_SIZE + 2;
			}
		}
		
		btnCombine = new RedButton(""){
			Image arrow;
			
			@Override
			protected void createChildren() {
				super.createChildren();
				
				arrow = Icons.get(Icons.RESUME);
				add(arrow);
			}
			
			@Override
			protected void layout() {
				super.layout();
				arrow.x = x + (width - arrow.width)/2f;
				arrow.y = y + (height - arrow.height)/2f;
				PixelScene.align(arrow);
			}
			
			@Override
			public void enable(boolean value) {
				super.enable(value);
				if (value){
					arrow.tint(1, 1, 0, 1);
					arrow.alpha(1f);
					bg.alpha(1f);
				} else {
					arrow.color(0, 0, 0);
					arrow.alpha(0.6f);
					bg.alpha(0.6f);
				}
			}
			
			@Override
			protected void onClick() {
				super.onClick();
				combine();
			}
		};
		btnCombine.enable(false);
		btnCombine.setRect(left + (w-30)/2f, inputs[1].top()+5, 30, inputs[1].height()-10);
		add(btnCombine);
		
		output = new ItemSlot(){
			@Override
			protected void onClick() {
				super.onClick();
				if (visible && item.trueName() != null){
					AlchemyScene.this.addToFront(new WndInfoItem(item));
				}
			}
		};
		output.setRect(left + w - BTN_SIZE - 10, inputs[1].top(), BTN_SIZE, BTN_SIZE);
		
		ColorBlock outputBG = new ColorBlock(output.width(), output.height(), 0x9991938C);
		outputBG.x = output.left();
		outputBG.y = output.top();
		add(outputBG);
		
		add(output);
		output.visible = false;
		
		bubbleEmitter = new Emitter();
		smokeEmitter = new Emitter();
		bubbleEmitter.pos(0, 0, Camera.main.width, Camera.main.height);
		smokeEmitter.pos(outputBG.x + (BTN_SIZE-16)/2f, outputBG.y + (BTN_SIZE-16)/2f, 16, 16);
		bubbleEmitter.autoKill = false;
		smokeEmitter.autoKill = false;
		add(bubbleEmitter);
		add(smokeEmitter);
		
		pos += 4;
		
		float btnWidth = (w-14)/2f;
		
		RedButton btnRecipes = new RedButton(Messages.get(AlchemyScene.class, "recipes_title")){
			@Override
			protected void onClick() {
				super.onClick();
				AlchemyScene.this.addToFront(new WndDocument(Document.ALCHEMY_GUIDE));
			}
		};
		btnRecipes.setRect(left + 5, pos, btnWidth, 18);
		PixelScene.align(btnRecipes);
		add(btnRecipes);
		
		RedButton btnClose = new RedButton(Messages.get(AlchemyScene.class, "close")){
			@Override
			protected void onClick() {
				super.onClick();
				Game.switchScene(GameScene.class);
			}
		};
		btnClose.setRect(left + w - 5 - btnWidth, pos, btnWidth, 18);
		PixelScene.align(btnClose);
		add(btnClose);
		
		pos = (int)btnClose.bottom() + 20;
		
		lowerBubbles = new Emitter();
		lowerBubbles.pos(0, pos, Camera.main.width, Math.max(0, Camera.main.height-pos));
		add(lowerBubbles);
		lowerBubbles.pour(Speck.factory( Speck.BUBBLE ), 0.1f );
		
		fadeIn();
	}
	
	@Override
	public void update() {
		super.update();
		water.offset( 0, -5 * Game.elapsed );
	}
	
	@Override
	protected void onBackPressed() {
		Game.switchScene(GameScene.class);
	}
	
	protected WndBag.Listener itemSelector = new WndBag.Listener() {
		@Override
		public void onSelect( Item item ) {
			synchronized (inputs) {
				if (item != null && inputs[0] != null) {
					for (int i = 0; i < inputs.length; i++) {
						if (inputs[i].item == null) {
							if (item instanceof Dart) {
								inputs[i].item(item.detachAll(Dungeon.hero.belongings.backpack));
							} else {
								inputs[i].item(item.detach(Dungeon.hero.belongings.backpack));
							}
							break;
						}
					}
					updateState();
				}
			}
		}
	};
	
	private<T extends Item> ArrayList<T> filterInput(Class<? extends T> itemClass){
		ArrayList<T> filtered = new ArrayList<>();
		for (int i = 0; i < inputs.length; i++){
			Item item = inputs[i].item;
			if (item != null && itemClass.isInstance(item)){
				filtered.add((T)item);
			}
		}
		return filtered;
	}
	
	private void updateState(){
		
		ArrayList<Item> ingredients = filterInput(Item.class);
		Recipe recipe = Recipe.findRecipe(ingredients);
		
		if (recipe != null){
			output.item(recipe.sampleOutput(ingredients));
			output.visible = true;
			btnCombine.enable(true);
			
		} else {
			btnCombine.enable(false);
			output.visible = false;
		}
		
	}
	
	private void combine(){
		
		ArrayList<Item> ingredients = filterInput(Item.class);
		Recipe recipe = Recipe.findRecipe(ingredients);
		
		Item result = null;
		
		if (recipe != null){
			result = recipe.brew(ingredients);
		}
		
		if (result != null){
			bubbleEmitter.start(Speck.factory( Speck.BUBBLE ), 0.01f, 100 );
			smokeEmitter.burst(Speck.factory( Speck.WOOL ), 10 );
			Sample.INSTANCE.play( Assets.SND_PUFF );
			
			output.item(result);
			if (!result.collect()){
				Dungeon.level.drop(result, Dungeon.hero.pos);
			}
			
			try {
				Dungeon.saveAll();
			} catch (IOException e) {
				ShatteredPixelDungeon.reportException(e);
			}
			
			synchronized (inputs) {
				for (int i = 0; i < inputs.length; i++) {
					if (inputs[i] != null && inputs[i].item != null) {
						if (inputs[i].item.quantity() <= 0) {
							inputs[i].slot.item(new WndBag.Placeholder(ItemSpriteSheet.SOMETHING));
							inputs[i].item = null;
						} else {
							inputs[i].slot.item(inputs[i].item);
						}
					}
				}
			}
			
			btnCombine.enable(false);
		}
		
	}
	
	@Override
	public void destroy() {
		synchronized ( inputs ) {
			for (int i = 0; i < inputs.length; i++) {
				if (inputs[i] != null && inputs[i].item != null) {
					if (!inputs[i].item.collect()) {
						Dungeon.level.drop(inputs[i].item, Dungeon.hero.pos);
					}
				}
				inputs[i] = null;
			}
		}
		try {
			Dungeon.saveAll();
			Badges.saveGlobal();
			Journal.saveGlobal();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
		super.destroy();
	}
	
	public static class ItemButton extends Component {
		
		protected NinePatch bg;
		protected ItemSlot slot;
		
		public Item item = null;
		
		@Override
		protected void createChildren() {
			super.createChildren();
			
			bg = Chrome.get( Chrome.Type.BUTTON );
			add( bg );
			
			slot = new ItemSlot() {
				@Override
				protected void onTouchDown() {
					bg.brightness( 1.2f );
					Sample.INSTANCE.play( Assets.SND_CLICK );
				};
				@Override
				protected void onTouchUp() {
					bg.resetColor();
				}
				@Override
				protected void onClick() {
					ItemButton.this.onClick();
				}
			};
			slot.enable(true);
			add( slot );
		}
		
		protected void onClick() {};
		
		@Override
		protected void layout() {
			super.layout();
			
			bg.x = x;
			bg.y = y;
			bg.size( width, height );
			
			slot.setRect( x + 2, y + 2, width - 4, height - 4 );
		};
		
		public void item( Item item ) {
			slot.item( this.item = item );
		}
	}
}