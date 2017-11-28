package spirite.base.image_data.mediums.maglev;

import java.util.ArrayList;
import java.util.List;

import spirite.base.image_data.ImageWorkspace;
import spirite.base.image_data.UndoEngine;
import spirite.base.image_data.ImageWorkspace.BuildingMediumData;
import spirite.base.image_data.UndoEngine.ImageAction;
import spirite.base.image_data.mediums.ABuiltMediumData;
import spirite.base.image_data.mediums.drawer.IImageDrawer.IAnchorLiftModule;
import spirite.base.image_data.mediums.drawer.IImageDrawer.ILiftSelectionModule;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill;
import spirite.base.image_data.mediums.maglev.parts.MagLevStroke;
import spirite.base.image_data.mediums.maglev.parts.MagLevFill.StrokeSegment;
import spirite.base.image_data.selection.ALiftedData;
import spirite.base.image_data.selection.SelectionMask;
import spirite.base.pen.StrokeEngine.IndexedDrawPoints;
import spirite.base.util.linear.MatTrans;

public class MLDModuleLift implements ILiftSelectionModule, IAnchorLiftModule
{

	private final BuildingMediumData building;
	private final MaglevMedium img;
	
	public MLDModuleLift( MaglevMedium img, BuildingMediumData building) {
		this.img = img;
		this.building = building;
	}

	@Override
	public boolean acceptsLifted(ALiftedData lifted) {
		return (lifted instanceof MaglevLiftedData);
	}

	@Override
	public void anchorLifted(ALiftedData lifted, MatTrans trans) {
		MaglevLiftedData mlift = (MaglevLiftedData)lifted;
		trans.translate(-mlift.iox, -mlift.ioy);

		UndoEngine undoEngine = building.handle.getContext().getUndoEngine();
		undoEngine.doAsAggregateAction(() -> {
			for( AMagLevThing thing : mlift.medium.things) {
				thing.transform(trans);
				

				undoEngine.performAndStore(new ImageAction(building) {
					@Override
					protected void performImageAction(ABuiltMediumData built) {
						ImageWorkspace ws = built.handle.getContext();
						MaglevMedium mimg = (MaglevMedium)ws.getData(built.handle);
						mimg.addThing(thing, false);
					}
				});
			}
			undoEngine.performAndStore(new ImageAction(building) {
				@Override
				protected void performImageAction(ABuiltMediumData built) {
					ImageWorkspace ws = built.handle.getContext();
					MaglevMedium mimg = (MaglevMedium)ws.getData(built.handle);
					mimg.unbuild();
					mimg.Build();
				}
			});
		}, "Anchor MagLev Lift");
	}

	@Override
	public ALiftedData liftSelection(SelectionMask selection) {
		UndoEngine undoEngine = building.handle.getContext().getUndoEngine();


		List<AMagLevThing> phaseOne  = new ArrayList<>();
		for( AMagLevThing thing : img.things) {
			if( thing instanceof MagLevStroke) {
				MagLevStroke stroke = (MagLevStroke)thing;
				IndexedDrawPoints direct = stroke.getDirect();
				
				for( int i=0; i < direct.length; ++i) {
					if( selection.contains((int)direct.x[i], (int)direct.y[i])) {
						phaseOne.add(thing);
						break;
					}
				}
			}
		}
		for( AMagLevThing thing : img.things) {
			if( thing instanceof MagLevFill) {
				MagLevFill fill = (MagLevFill)thing;
				
				// Would be so much easier with Linq
				boolean fullLiftFill = true;
				for( StrokeSegment ss : fill.segments) {
					boolean isBeingRemoved = false;
					for( AMagLevThing remove : phaseOne) {
						if( remove.id == ss.strokeId) {
							isBeingRemoved = true;
						}
					}
					if( !isBeingRemoved) {
						fullLiftFill = false;
						break;
					}
				}
				
				if( fullLiftFill) phaseOne.add(fill);
			}
		}
		
		// Sort things by order they appear in the original maglev
		List<AMagLevThing> toRemove = new ArrayList<>();
		for( AMagLevThing thing : img.things) {
			if( phaseOne.contains(thing))
				toRemove.add(thing);
		}
		
		if( toRemove.size() != 0) {
			undoEngine.performAndStore(new ImageAction(building) {
				@Override
				protected void performImageAction(ABuiltMediumData built) {
					ImageWorkspace ws = built.handle.getContext();
					MaglevMedium mimg = (MaglevMedium)ws.getData(built.handle);
	
					for( AMagLevThing thing : toRemove)
						mimg.removeThing(thing);
					
					mimg.unbuild();
					mimg.Build();
				}
			});
	
			List<AMagLevThing> toLift = new ArrayList<>(toRemove.size());
			for( AMagLevThing thing : toRemove)
				toLift.add(thing.clone());
			return new MaglevLiftedData(new MaglevMedium(building.handle.getContext(), toLift), selection);
		}
		
		return null;
	}
}
