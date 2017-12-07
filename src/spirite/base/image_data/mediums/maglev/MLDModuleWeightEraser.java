package spirite.base.image_data.mediums.maglev;

import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.UndoEngine.StackableAction;
import spirite.base.image_data.UndoEngine.UndoableAction;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IWeightEraserModule;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.pen.DrawPoints;
import spirite.base.pen.IndexedDrawPoints;
import spirite.base.util.MUtil;
import spirite.base.util.compaction.FloatCompactor;

import java.util.ArrayList;
import java.util.List;

class MLDModuleWeightEraser implements IWeightEraserModule{

	private final BuildingMediumData building;
	private final MaglevMedium img;
	
	public MLDModuleWeightEraser( MaglevMedium img, BuildingMediumData building) {
		this.img = img;
		this.building = building;
	}
	

	static Object iweIDLocker = new Object();
	static int iweIDCounter = 0;
	boolean weightPrecise;
	int iweId = -1;
	@Override
	public void weightErase(float x, float y, float w) {
		ImageWorkspace ws = this.building.handle.getContext();
		
		if (weightPrecise) {
			List<EraseAction> toRemove = new ArrayList<>();
			

			for( AMagLevThing thing : img.getThings()) {
				if( thing instanceof MagLevStroke) {
					FloatCompactor fc = new FloatCompactor(16);
					
					boolean inStroke = true;
					
					MagLevStroke stroke = (MagLevStroke)thing;
					IndexedDrawPoints direct = stroke.getDirect();
					
					for(int i = 0; i < direct.getLength(); ++i) {
						if( MUtil.distance(x, y, direct.getX()[i], direct.getY()[i]) < w) {
							if( inStroke) {
								inStroke = false;
								fc.add(direct.getT()[i]);
							}
						}
						else if(!inStroke) {
							inStroke = true;
							fc.add(direct.getT()[i]);
						}
					}
					
					if( fc.size() > 0)
						toRemove.add(new EraseAction(thing.id, fc.toArray()));
				}
			}
			
			if( toRemove.size() != 0) {
				ws.getUndoEngine().performAndStore(
						new PreciseMagWeightEraseAction(building, toRemove, iweId));
			}
		}
		else {
			List<AMagLevThing> thingsToRemove = new ArrayList<>();
			
			for( AMagLevThing thing : img.getThings()) {
				if( thing instanceof MagLevStroke) {
					MagLevStroke stroke = (MagLevStroke)thing;
					DrawPoints direct = stroke.getDirect();

					for(int i = 0; i < direct.getLength(); ++i) {
						if( MUtil.distance(x, y, direct.getX()[i], direct.getY()[i]) < w) {
							thingsToRemove.add(stroke);
							break;
						}
					}
				}
			}
			
			if( thingsToRemove.size() > 0) {
				ws.getUndoEngine().performAndStore(
						new MagWeightEraseAction(building, thingsToRemove, iweId));
			}
		}
	}

	@Override
	public void startWeightErase(boolean precise) {
		weightPrecise = precise;
		synchronized( iweIDLocker) {
			iweId = iweIDCounter++;
		}
	}
	@Override
	public void endWeightErase() {
		iweId = -1;
	}

	private static class EraseAction {
		private final int strokeId;
		private final float[] remaining;
		
		private EraseAction( int strokeId, float[] remaining) {
			this.strokeId = strokeId;
			this.remaining = remaining;
		}
	}
	
	class PreciseMagWeightEraseAction extends ImageAction
		implements StackableAction
	{
		private final int id;
		private final List<EraseAction> erases = new ArrayList<>();
		
		
		protected PreciseMagWeightEraseAction(BuildingMediumData data, List<EraseAction> erases, int weid) {
			super(data);
			this.id = weid;
			this.erases.addAll(erases);
		}


		@Override
		protected void performImageAction(ABuiltMediumData built) {
			ImageWorkspace ws = built.handle.getContext();
			MaglevMedium mimg = (MaglevMedium)ws.getData(built.handle);
			
			for( EraseAction erase : erases) {
				mimg.splitStroke(erase.strokeId, erase.remaining);
			}
			mimg.unbuild();
			mimg.Build();
		}
		

		@Override public boolean canStack(UndoableAction newAction) {
			return ( newAction instanceof PreciseMagWeightEraseAction && ((PreciseMagWeightEraseAction) newAction).id == id);
		}

		@Override
		public void stackNewAction(UndoableAction newAction) {
			erases.addAll(((PreciseMagWeightEraseAction)newAction).erases);
		}
	
	}
	
	class MagWeightEraseAction extends ImageAction
		implements StackableAction 
	{
		private final List<AMagLevThing> thingsToErase;
		private final int id;
		MagWeightEraseAction(BuildingMediumData data, List<AMagLevThing> thingsToErase, int id)
		{
			super(data);
			this.id = id;
			this.thingsToErase = new ArrayList<>(1);
			this.thingsToErase.addAll(thingsToErase);
		}

		@Override public boolean canStack(UndoableAction newAction) {
			return ( newAction instanceof MagWeightEraseAction && ((MagWeightEraseAction) newAction).id == id);
		}
		@Override public void stackNewAction(UndoableAction newAction) {
			thingsToErase.addAll(((MagWeightEraseAction)newAction).thingsToErase);
		}

		@Override
		protected void performImageAction(ABuiltMediumData built) {
			ImageWorkspace ws = building.handle.getContext();
			MaglevMedium mimg = (MaglevMedium)ws.getData(building.handle);
			
			for( AMagLevThing toErase : thingsToErase) {
				mimg.removeThing(toErase);
			}
		}
		@Override
		public String getDescription() {
			return "Erase Mag Stroke";
		}
	}
}
