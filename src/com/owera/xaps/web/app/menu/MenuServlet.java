package com.owera.xaps.web.app.menu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.owera.common.log.Logger;
import com.owera.xaps.dbi.Users;
import com.owera.xaps.web.Page;
import com.owera.xaps.web.app.page.report.ReportType;
import com.owera.xaps.web.app.util.Freemarker;
import com.owera.xaps.web.app.util.SessionCache;
import com.owera.xaps.web.app.util.SessionData;
import com.owera.xaps.web.app.util.WebProperties;
import com.thoughtworks.xstream.XStream;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * A menu servlet that generates different types of response based a type parameter.
 * 
 * @author Jarl Andre Hubenthal
 *
 */
public class MenuServlet extends HttpServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1057185634997595190L;

	/** The template config. */
	private static Configuration templateConfig;

	/** The logger. */
	private static Logger logger = new Logger();

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 */
	public void init() {
		getTemplateConfig();
	}

	/**
	 * Gets the template config.
	 *
	 * @return the template config
	 */
	private static Configuration getTemplateConfig() {
		if (templateConfig == null)
			templateConfig = Freemarker.initFreemarkerForClassLoading(MenuServlet.class);
		return templateConfig;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	@Deprecated
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String type = req.getParameter("type");
		if (type == null)
			type = "html";
		//		
		//		ResponseCache.addNoCacheToResponse(res);

		try {
			if (type.equals("xml")) {
				XStream xstream = new XStream();
				xstream.alias("menuitem", MenuItem.class);
				xstream.alias("attribute", MenuItemAttribute.class);
				List<MenuItem> mainMenu = getMainMenu(req);
				String xml = xstream.toXML(mainMenu);
				res.setContentType("application/xml");
				res.getWriter().println(xml);
				res.getWriter().close();
			} else if (type.equals("json")) {
				flexjson.JSONSerializer serializer = new flexjson.JSONSerializer();
				List<MenuItem> mainMenu = getMainMenu(req);
				String json = serializer.deepSerialize(mainMenu);
				res.setContentType("application/json");
				res.getWriter().println(json);
				res.getWriter().close();
			} else if (type.equals("html")) {
				String html = getMenuHtml(req);
				res.setContentType("text/html");
				res.getWriter().println(html);
				res.getWriter().close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("An error occured", e);
		}
	}

	/**
	 * Gets the menu html.
	 *
	 * @param req the req
	 * @return the menu html
	 * @throws TemplateException the template exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String getMenuHtml(HttpServletRequest req) throws TemplateException, IOException {
		List<MenuItem> mainMenu = getMainMenu(req);
		Template template = getTemplateConfig().getTemplate("MenuTemplate.ftl");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("list", mainMenu);
		String html = Freemarker.parseTemplate(map, template);
		return html;
	}

	/**
	 * Gets the main menu.
	 *
	 * @param req the req
	 * @return the main menu
	 */
	private static List<MenuItem> getMainMenu(HttpServletRequest req) {
		String page = req.getParameter("page");

		Page selectedMenuPage = Page.getById(Page.getParentPage(page));

		List<MenuItem> mainMenu = getMenuItems(req.getSession().getId(), page, selectedMenuPage);//new ArrayList<MenuItem>();

		return mainMenu;
	}

	/**
	 * Gets the pages allowed.
	 *
	 * @param sessionId the session id
	 * @return the pages allowed
	 */
	private static List<String> getPagesAllowed(String sessionId) {
		SessionData sessionData = SessionCache.getSessionData(sessionId);
		if (sessionData.getUser() != null && !sessionData.getUser().getAccess().equals(Users.ACCESS_ADMIN))
			return sessionData.getUser().getAllowedPages();
		else if (sessionData.getUser() == null || (sessionData.getUser() != null && sessionData.getUser().getAccess().equals(Users.ACCESS_ADMIN)))
			return Page.getAllPagesAsString();
		return null;
	}

	/**
	 * Gets the menu items.
	 *
	 * @param sessionId the session id
	 * @param currentPageId the current page id
	 * @param selectedPage the selected page
	 * @return the menu items
	 */
	public static List<MenuItem> getMenuItems(String sessionId, String currentPageId, Page selectedPage) {
		List<Page> allowedPages = Page.getPageValuesFromList(getPagesAllowed(sessionId));
		//		Boolean isCurrentGroup = isCurrentGroup(sessionId);
		//		Boolean isCurrentJob = isCurrentJob(sessionId);
		return createMenuItems(allowedPages, selectedPage, sessionId);
	}

	/**
	 * Gets the tools menu.
	 *
	 * Morten: Is there a need to know "currentPage"?
	 * @param sessionId the session id
	 * @param currentPage the current page
	 * @return the tools menu
	 */
	@SuppressWarnings("serial")
	public List<MenuItem> getToolsMenu(final String sessionId, final String currentPage) {
		final List<Page> _pages = Page.getPageValuesFromList(getPagesAllowed(sessionId));
		return new ArrayList<MenuItem>() {
			{
				if (_pages.contains(Page.PERMISSIONS))
					add(new MenuItem("Permissions", Page.PERMISSIONS).setSelected(currentPage.equals(Page.PERMISSIONS)));
				if (_pages.contains(Page.CERTIFICATES))
					add(new MenuItem("Certificates", Page.CERTIFICATES).setSelected(currentPage.equals(Page.CERTIFICATES)));
				if (_pages.contains(Page.MONITOR))
					add(new MenuItem("Monitor", Page.MONITOR).setSelected(currentPage.equals(Page.MONITOR)));
			}
		};
	}

	/**
	 * Checks if is current job.
	 *
	 * @param sessionId the session id
	 * @return the boolean
	 */
	//	private static Boolean isCurrentJob(String sessionId) {
	//		SessionData sessionData = SessionCache.getSessionData(sessionId);
	//		String name = sessionData.getJobname();
	//		String unittype = sessionData.getUnittypeName();
	//		return name != null && unittype != null && !unittype.equals(WebConstants.ALL_ITEMS_OR_DEFAULT);
	//	}

	/**
	 * Checks if is current group.
	 *
	 * @param sessionId the session id
	 * @return the boolean
	 */
	//	private static Boolean isCurrentGroup(String sessionId) {
	//		SessionData sessionData = SessionCache.getSessionData(sessionId);
	//		String group = sessionData.getGroup();
	//		String unittype = sessionData.getUnittypeName();
	//		return group != null && !group.equals(WebConstants.ALL_ITEMS_OR_DEFAULT) && unittype != null && !unittype.equals(WebConstants.ALL_ITEMS_OR_DEFAULT);
	//	}

	/**
	 * Creates the menu items new standard.
	 *
	 * @param allowedPages the pages
	 * @param selectedPage the selected page
	 * @param isCurrentGroup the is current group
	 * @param isCurrentJob the is current job
	 * @param sessionId the session id
	 * @return the list
	 */
	private static List<MenuItem> createMenuItems(List<Page> allowedPages, Page selectedPage, String sessionId) {
		List<MenuItem> menu = new ArrayList<MenuItem>();
		if (allowedPages.contains(Page.DASHBOARD_SUPPORT)) {
			MenuItem support = new MenuItem("Support", Page.DASHBOARD_SUPPORT).setSelected(selectedPage.equalsAny(Page.SEARCH, Page.DASHBOARD_SUPPORT, Page.SYSLOG));
			if (allowedPages.contains(Page.SEARCH))
				support.addSubMenuItem(new MenuItem("Search", Page.SEARCH));
			if (allowedPages.contains(Page.SYSLOG))
				support.addSubMenuItem(new MenuItem("Syslog", Page.SYSLOG));
			support.addSubMenuItem(new MenuItem("Fusion-support","mailto:support@pingcom.net?subject=Suggestions,%20improvements%20or%20bugs%20in%20Fusion&body=To%20Fusion%20Support"));
			menu.add(support);
		}
		if (allowedPages.contains(Page.TOPMENU_EASY)) {
			MenuItem simpleProv = new MenuItem("Easy Provisioning", Page.TOPMENU_EASY).setSelected(selectedPage.equalsAny(Page.UNITTYPE, Page.PROFILE, Page.UNIT)).setDisableOnClickWithJavaScript();
			if (allowedPages.contains(Page.UNITTYPE))
				simpleProv.addSubMenuItem(new MenuItem("Unit Type", Page.UNITTYPEOVERVIEW).addSubMenuItems(new MenuItem("Unit Type Overview", Page.UNITTYPEOVERVIEW), new MenuItem("Create Unit Type",
						Page.UNITTYPECREATE)));
			if (allowedPages.contains(Page.PROFILE))
				simpleProv.addSubMenuItem(new MenuItem("Profile", Page.PROFILEOVERVIEW).addSubMenuItems(new MenuItem("Profile Overview", Page.PROFILEOVERVIEW), new MenuItem("Create Profile",
						Page.PROFILECREATE)));
			if (allowedPages.contains(Page.UNIT))
				simpleProv.addSubMenuItem(new MenuItem("Unit", Page.UNIT).addSubMenuItems(new MenuItem("Create Unit", Page.UNIT).addCommand("create")));
			menu.add(simpleProv);
		}
		if (allowedPages.contains(Page.TOPMENU_ADV)) {
			MenuItem advProv = new MenuItem("Advanced Provisioning", Page.TOPMENU_ADV).setSelected(selectedPage.equalsAny(Page.GROUP, Page.JOB)).setDisableOnClickWithJavaScript();
			if (allowedPages.contains(Page.GROUP))
				advProv.addSubMenuItem(new MenuItem("Group", Page.GROUPSOVERVIEW).addSubMenuItems(new MenuItem("Group Overview", Page.GROUPSOVERVIEW),
						new MenuItem("Create Group", Page.GROUP).addCommand("create")));
			if (allowedPages.contains(Page.JOB))
				advProv.addSubMenuItem(new MenuItem("Job", Page.JOBSOVERVIEW).addSubMenuItems(new MenuItem("Job Overview", Page.JOBSOVERVIEW),
						new MenuItem("Create Job", Page.JOB).addCommand("create")));
			menu.add(advProv);
		}
		if (allowedPages.contains(Page.TOPMENU_FILESCRIPT)) {
			MenuItem filescript = new MenuItem("Files & Scripts", Page.TOPMENU_FILESCRIPT).setSelected(selectedPage.equalsAny(Page.FILES, Page.SCRIPTEXECUTIONS)).setDisableOnClickWithJavaScript();
			if (allowedPages.contains(Page.FILES))
				filescript.addSubMenuItem(new MenuItem("Files", Page.FILES));
			filescript.addSubMenuItem(new MenuItem("Script Executions", Page.SCRIPTEXECUTIONS));

			menu.add(filescript);
		}
		if (allowedPages.contains(Page.TOPMENU_TRIGEVENT)) {
			MenuItem triggerAndEvents = new MenuItem("Triggers & Events", Page.TOPMENU_TRIGEVENT).setSelected(
					selectedPage.equalsAny(Page.TRIGGEROVERVIEW, Page.CREATETRIGGER, Page.TRIGGERRELEASE, Page.TRIGGERRELEASEHISTORY, Page.SYSLOGEVENTS, Page.HEARTBEATS))
					.setDisableOnClickWithJavaScript();
			triggerAndEvents.addSubMenuItem(new MenuItem("Triggers", Page.TRIGGEROVERVIEW));
			//			triggerAndEvents.addSubMenuItem(new MenuItem("Create Trigger", Page.CREATETRIGGER));
			triggerAndEvents.addSubMenuItem(new MenuItem("Releases", Page.TRIGGERRELEASE));
			triggerAndEvents.addSubMenuItem(new MenuItem("Release History", Page.TRIGGERRELEASEHISTORY));
			triggerAndEvents.addSubMenuItem(new MenuItem("Syslog Events", Page.SYSLOGEVENTS));
			triggerAndEvents.addSubMenuItem(new MenuItem("Heartbeats", Page.HEARTBEATS));
			menu.add(triggerAndEvents);
		}

		if (allowedPages.contains(Page.TOPMENU_REPORT)) {
			MenuItem reporting = new MenuItem("Reports", Page.TOPMENU_REPORT).setSelected(selectedPage.equalsAny(Page.REPORT)).setDisableOnClickWithJavaScript();
			MenuItem fusionReports = new MenuItem("Fusion", Page.REPORT).setDisableOnClickWithJavaScript();
			reporting.addSubMenuItem(fusionReports);
			fusionReports.addSubMenuItem(new MenuItem("Unit", Page.REPORT).addParameter("type", ReportType.UNIT.getName()));
			fusionReports.addSubMenuItem(new MenuItem("Group", Page.REPORT).addParameter("type", ReportType.GROUP.getName()));
			fusionReports.addSubMenuItem(new MenuItem("Job", Page.REPORT).addParameter("type", ReportType.JOB.getName()));
			fusionReports.addSubMenuItem(new MenuItem("Provisioning", Page.REPORT).addParameter("type", ReportType.PROV.getName()));
			MenuItem syslogReport = new MenuItem("Syslog", Page.REPORT.getUrl("type=" + ReportType.SYS.getName()));
			fusionReports.addSubMenuItem(syslogReport);
			syslogReport.addSubMenuItem(new MenuItem("Units", Page.UNITLIST.getUrl("type=" + ReportType.SYS.getName()), new ArrayList<MenuItem>()));

			MenuItem syslogReports = new MenuItem("Pingcom Devices", Page.REPORT);
			MenuItem voipReport = new MenuItem("Voip", Page.REPORT.getUrl("type=" + ReportType.VOIP.getName()));
			syslogReports.addSubMenuItem(voipReport);
			voipReport.addSubMenuItem(new MenuItem("Units", Page.UNITLIST.getUrl("type=" + ReportType.VOIP.getName()), new ArrayList<MenuItem>()));
			MenuItem hardwareReport = new MenuItem("Hardware", Page.REPORT.getUrl("type=" + ReportType.HARDWARE.getName()));
			syslogReports.addSubMenuItem(hardwareReport);
			hardwareReport.addSubMenuItem(new MenuItem("Units", Page.UNITLIST.getUrl("type=" + ReportType.HARDWARE.getName()), new ArrayList<MenuItem>()));
			syslogReports.setDisableOnClickWithJavaScript();
			reporting.addSubMenuItem(syslogReports);

			//			MenuItem trReports = new MenuItem("TR069 Devices", Page.REPORT);
			//			reporting.addSubMenuItem(trReports);
			//			trReports.addSubMenuItem(new MenuItem("Voip", Page.REPORT.getUrl("type=" + ReportType.VOIPTR.getName()), new ArrayList<MenuItem>()));
			//			trReports.addSubMenuItem(new MenuItem("Hardware", Page.REPORT.getUrl("type=" + ReportType.HARDWARETR.getName()), new ArrayList<MenuItem>()));
			//			trReports.addSubMenuItem(new MenuItem("Gateway", Page.REPORT.getUrl("type=" + ReportType.GATEWAYTR.getName()), new ArrayList<MenuItem>()));
			//			trReports.setDisableOnClickWithJavaScript();
			menu.add(reporting);
		}
		if (allowedPages.contains(Page.TOPMENU_WIZARDS)) {
			MenuItem wizards = new MenuItem("Wizards", Page.TOPMENU_WIZARDS).setSelected(selectedPage.equalsAny(Page.UPGRADE)).setDisableOnClickWithJavaScript();
			wizards.addSubMenuItem(new MenuItem("Upgrade wizard", Page.UPGRADE));
			menu.add(wizards);
		}
		if (WebProperties.getWebProperties().getBoolean("staging.enabled")) {
			MenuItem staging = new MenuItem("Staging", Page.TOPMENU_STAGING).setSelected(
					selectedPage.equalsAny(Page.STAGINGDISTRIBUTORS, Page.STAGINGPROVIDERS, Page.STAGINGRETURN, Page.STAGINGSHIPMENTS)).setDisableOnClickWithJavaScript();
			//			staging.addSubMenuItem(new MenuItem("Manage distributors", Page.STAGINGDISTRIBUTORS));
			//			staging.addSubMenuItem(new MenuItem("Manage providers", Page.STAGINGPROVIDERS));
			//			staging.addSubMenuItem(new MenuItem("Return units", Page.STAGINGRETURN));
			staging.addSubMenuItem(new MenuItem("Ship units", Page.STAGINGSHIPMENTS));
			menu.add(staging);
		}
		return menu;
	}
}