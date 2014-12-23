package gov.nasa.worldwind.render.city;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.render.airspaces.editor.CityPolygonEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ListSelectionModel;

import org.postgis.Geometry;
import org.postgis.MultiPolygon;
import org.postgis.PGgeometry;
import org.postgis.Point;

public class ArchitectureBuildController implements ActionListener {

	private CityBuilderController controller;

	public ArchitectureBuildController(CityBuilderController controller) {
		this.controller = controller;
	}

	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	public void openArchitectureData() {
		final ArrayList<CityPolygon> polygons = new ArrayList<CityPolygon>();
		try {
			Statement s = this.controller.getApp().conn.createStatement();
			ResultSet r = s.executeQuery("select gid, the_geom from building");
			int ii = 0;
			while (r.next()) {
				ii++;
				ArrayList<LatLon> positions = new ArrayList<LatLon>();
				PGgeometry geom = (PGgeometry) r.getObject(2);
				String gid;
				gid = r.getObject(1).toString();

				if (geom.getGeometry().getType() == Geometry.MULTIPOLYGON) {
					MultiPolygon pg = (MultiPolygon) geom.getGeometry();
					LatLon latlon;

					for (int i = 0; i < pg.numPoints(); i++) {
						Point point = pg.getPoint(i);
						latlon = LatLon.fromDegrees(point.y, point.x);
						positions.add(latlon);

					}
				}

				CityPolygon polygon = new CityPolygon(positions);
				polygon.setAltitudes(0, 10);
				polygon.setTerrainConforming(true, true);
			    
				polygons.add(polygon);
				polygon.setValue(AVKey.DISPLAY_NAME, gid);
				// ShapeAttributes attributes = new BasicShapeAttributes();
				// polyline.setAttributes(attributes);

			}
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.controller.setPolygons(polygons);
		this.controller.setEnabled(true);
		this.controller.getApp().setCursor(null);
		this.controller.getApp().getWwd().redraw();

	}

	public void newArchitectureDate() {
		if (this.controller.getArchitectureView().getSelectedFactory().equals("House")) this.createNewPgEntry(CityBuilder.defaultArchitectureFactories[0]);
    	if (this.controller.getArchitectureView().getSelectedFactory().equals("Factory")) this.createNewPgEntry(CityBuilder.defaultArchitectureFactories[1]);
    	if (this.controller.getArchitectureView().getSelectedFactory().equals("School")) this.createNewPgEntry(CityBuilder.defaultArchitectureFactories[2]);
	}
	
    public void createNewPgEntry(PolygonFactory factory)
    {
    	CityPolygon cityPolygon = factory.createPolygon(this.controller.getApp().getWwd(), this.controller.isResizeNewShapesToViewport());
        CityPolygonEditor editor = factory.createEditor(cityPolygon);
        PolygonEntry entry = new PolygonEntry(cityPolygon, editor);
        this.controller.addPgEntry(entry);
        this.controller.selectPgEntry(entry, true);
    }

	public void saveArchitectureDate() {
		;
	}

	public void deleteArchitectureDate() {
		   this.controller.removePgEntries(Arrays.asList(this.controller.getPgSelectedEntries()));
	}

	public void deSelectArchitectureDate() {
		this.controller.selectPgEntry(null, true);
	}

	public void resizeArchitectureDate() {
		  if(this.controller.getArchitectureView().resizeAction.getValue("SmallIcon")==this.controller.getArchitectureView().iconResize )
		  {this.controller.getArchitectureView().resizeAction.putValue("SmallIcon", this.controller.getArchitectureView().iconResize_g);
		  this.controller.setResizeNewShapesToViewport(false);
		  }
		  else if (this.controller.getArchitectureView().resizeAction.getValue("SmallIcon")==this.controller.getArchitectureView().iconResize_g )
		  {this.controller.getArchitectureView().resizeAction.putValue("SmallIcon", this.controller.getArchitectureView().iconResize);
		   this.controller.setResizeNewShapesToViewport(true);
		  }
		
	}

	public void modifyArchitectureDate() {
		 if(this.controller.getArchitectureView().modifyAction.getValue("SmallIcon")==this.controller.getArchitectureView().iconModify )
		  {this.controller.getArchitectureView().modifyAction.putValue("SmallIcon", this.controller.getArchitectureView().iconModify_g);
		   this.controller.setPgEnableEdit(false);
		   this.controller.getArchitectureView().entryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		  }
		  else if (this.controller.getArchitectureView().modifyAction.getValue("SmallIcon")==this.controller.getArchitectureView().iconModify_g )
		  {this.controller.getArchitectureView().modifyAction.putValue("SmallIcon", this.controller.getArchitectureView().iconModify );
		   this.controller.setPgEnableEdit(true);
		   this.controller.getArchitectureView().entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		  } 
	}

	public void sqlArchitectureDate() {
		;
	}

}
