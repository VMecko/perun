package cz.metacentrum.perun.webgui.tabs.vostabs;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.*;
import cz.metacentrum.perun.webgui.client.PerunWebSession;
import cz.metacentrum.perun.webgui.client.UiElements;
import cz.metacentrum.perun.webgui.client.localization.ButtonTranslation;
import cz.metacentrum.perun.webgui.client.mainmenu.MainMenu;
import cz.metacentrum.perun.webgui.client.resources.*;
import cz.metacentrum.perun.webgui.json.GetEntityById;
import cz.metacentrum.perun.webgui.json.JsonCallbackEvents;
import cz.metacentrum.perun.webgui.json.JsonUtils;
import cz.metacentrum.perun.webgui.json.authzResolver.GetRichAdminsWithAttributes;
import cz.metacentrum.perun.webgui.json.authzResolver.RemoveAdmin;
import cz.metacentrum.perun.webgui.model.User;
import cz.metacentrum.perun.webgui.model.VirtualOrganization;
import cz.metacentrum.perun.webgui.tabs.*;
import cz.metacentrum.perun.webgui.tabs.userstabs.UserDetailTabItem;
import cz.metacentrum.perun.webgui.widgets.CustomButton;
import cz.metacentrum.perun.webgui.widgets.TabMenu;

import java.util.ArrayList;
import java.util.Map;

/**
 * VO admins
 * 
 * @author Vaclav Mach <374430@mail.muni.cz>
 * @author Pavel Zlamal <256627@mail.muni.cz>
 * @version $Id$
 */
public class VoManagersTabItem implements TabItem, TabItemWithUrl{

	/**
	 * Perun web session
	 */
	private PerunWebSession session = PerunWebSession.getInstance();
	
	/**
	 * Content widget - should be simple panel
	 */
	private SimplePanel contentWidget = new SimplePanel();
	
	/**
	 * Title widget
	 */
	private Label titleWidget = new Label("Loading vo managers");
	
	// data
	private VirtualOrganization vo;
	//
	private int voId;

	/**
	 * Creates a tab instance
	 *
     * @param vo
     */
	public VoManagersTabItem(VirtualOrganization vo){
		this.vo = vo;
		this.voId = vo.getId();
	}

    /**
     * Creates a tab instance
     *
     * @param voId
     */
    public VoManagersTabItem(int voId){
		this.voId = voId;
        JsonCallbackEvents events = new JsonCallbackEvents(){
            public void onFinished(JavaScriptObject jso) {
                vo = jso.cast();
            }
        };
        new GetEntityById(PerunEntity.VIRTUAL_ORGANIZATION, voId, events).retrieveData();
	}
	
	public boolean isPrepared(){
		return !(vo == null);
	}

	
	public Widget draw() {
		
		titleWidget.setText(Utils.getStrippedStringWithEllipsis(vo.getName())+": managers");
		
		// MAIN PANEL
		VerticalPanel firstTabPanel = new VerticalPanel();
		firstTabPanel.setSize("100%", "100%");

		// HORIZONTAL MENU
		TabMenu menu = new TabMenu();

		// members request
		final GetRichAdminsWithAttributes admins = new GetRichAdminsWithAttributes(PerunEntity.VIRTUAL_ORGANIZATION, voId, null);

		// Events for reloading when finished
		final JsonCallbackEvents events = JsonCallbackEvents.refreshTableEvents(admins);

		CustomButton addButton = TabMenu.getPredefinedButton(ButtonType.ADD, ButtonTranslation.INSTANCE.addManagerToVo(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                session.getTabManager().addTabToCurrentTab(new AddVoManagerTabItem(voId), true);
            }
        });
		menu.addWidget(addButton);


		final CustomButton removeButton = TabMenu.getPredefinedButton(ButtonType.REMOVE, ButtonTranslation.INSTANCE.removeManagerFromVo());
		menu.addWidget(removeButton);
        removeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				final ArrayList<User> adminsForRemoving = admins.getTableSelectedList();
                String text = "Following users won't be VO managers anymore.";
				UiElements.showDeleteConfirm(adminsForRemoving, text, new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        // TODO - SHOULD HAVE ONLY ONE CALLBACK TO CORE !!
                        for (int i=0; i<adminsForRemoving.size(); i++ ) {
                            RemoveAdmin request;
                            if(i == adminsForRemoving.size() - 1) {
                                request = new RemoveAdmin(PerunEntity.VIRTUAL_ORGANIZATION, JsonCallbackEvents.disableButtonEvents(removeButton, events));
                            } else {
                                request = new RemoveAdmin(PerunEntity.VIRTUAL_ORGANIZATION, JsonCallbackEvents.disableButtonEvents(removeButton));
                            }
                            request.removeAdmin(voId, adminsForRemoving.get(i).getId());
                        }
                    }
                });
			}
		});

        // get the table
        CellTable<User> table;
        if (session.isPerunAdmin()) {
            table = admins.getTable(new FieldUpdater<User, String>() {
                public void update(int i, User user, String s) {
                    session.getTabManager().addTab(new UserDetailTabItem(user));
                }
            });
        } else {
            table = admins.getTable();
        }

		// add a class to the table and wrap it into scroll panel
		table.addStyleName("perun-table");
		ScrollPanel sp = new ScrollPanel(table);
		sp.addStyleName("perun-tableScrollPanel");		

		// add menu and the table to the main panel
		firstTabPanel.add(menu);
		firstTabPanel.setCellHeight(menu, "30px");
		firstTabPanel.add(sp);

        removeButton.setEnabled(false);
        JsonUtils.addTableManagedButton(admins, table, removeButton);

		session.getUiElements().resizePerunTable(sp, 350, this);

		this.contentWidget.setWidget(firstTabPanel);
		
		return getWidget();
	}

	public Widget getWidget() {
		return this.contentWidget;
	}

	public Widget getTitle() {
		return this.titleWidget;
	}

	public ImageResource getIcon() {
		return SmallIcons.INSTANCE.administratorIcon(); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + voId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
        VoManagersTabItem other = (VoManagersTabItem) obj;
		if (voId != other.voId)
			return false;
		return true;
	}

	public boolean multipleInstancesEnabled() {
		return false;
	}
	
	public void open()
	{
		session.getUiElements().getMenu().openMenu(MainMenu.VO_ADMIN);
        session.getUiElements().getBreadcrumbs().setLocation(vo, "Managers", getUrlWithParameters());
		if(vo != null){
			session.setActiveVo(vo);
			return;
		}
		session.setActiveVoId(voId);
	}

	
	public boolean isAuthorized() {

		if (session.isVoAdmin(voId) ) {
			return true; 
		} else {
			return false;
		}

	}
	
	public final static String URL = "managers";
	
	public String getUrl()
	{
		return URL;
	}
	
	public String getUrlWithParameters()
	{
		return VosTabs.URL + UrlMapper.TAB_NAME_SEPARATOR + getUrl() + "?id=" + voId;
	}
	
	static public VoManagersTabItem load(Map<String, String> parameters)
	{
		int voId = Integer.parseInt(parameters.get("id"));
		return new VoManagersTabItem(voId);
	}

}