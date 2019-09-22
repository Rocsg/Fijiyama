import fiji.util.gui.OverlayedImageCanvas;
import trainableSegmentation.ImageOverlay;
import trainableSegmentation.RoiListOverlay;
import trainableSegmentation.WekaSegmentation;

for(int i = 0; i < WekaSegmentation.MAX_NUM_CLASSES; i++)
			{
				roiOverlay[i] = new RoiListOverlay();
				roiOverlay[i].setComposite( transparency050 );
				((OverlayedImageCanvas)ic).addOverlay(roiOverlay[i]);
			}

			// add result overlay
			resultOverlay = new ImageOverlay();
			resultOverlay.setComposite( overlayAlpha );
			((OverlayedImageCanvas)ic).addOverlay(resultOverlay);

			// Remove the canvas from the window, to add it later
			removeAll();
