package info.caq9;

import info.caq9.basic.WeiboLocation;
import info.caq9.basic.WeiboMessage;
import info.caq9.basic.WeiboUser;
import info.caq9.util.NumberParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Weibo Nearby page parser, main class for the project. Contains methods to get
 * the home page of this location, nearby locations, messages and users.
 * 
 * @author CAQ9
 * 
 */
public class WeiboNearbyParser {
	static final boolean DEBUG = true;

	static final JSONParser JSONPARSER = new JSONParser();

	/**
	 * Extract total count of a list (messages, locations, etc.)
	 * 
	 * @param html
	 *            The HTML source
	 * @return The parsed count
	 */
	private static Long extractTotalCount(String html) {
		int pos1 = html.indexOf("<div class=\"tab_radious\">");
		int pos2 = html.indexOf("</div>", pos1);
		int pos3 = html.indexOf("共", pos1) + 1;
		int pos4 = html.lastIndexOf("条", pos2);
		if (pos4 > pos3) {
			String total = html.substring(pos3, pos4);
			if (DEBUG)
				System.out.println(total);
			return new Long(total);
		}
		return null;
	}

	/**
	 * Extract basic meta-info (config) of the location and the user, including: <br/>
	 * $CONFIG['oid']='116.411126_39.913456'; <br/>
	 * $CONFIG['onick']='大阮府胡同'; <br/>
	 * $CONFIG['uid']='1982631825'; <br/>
	 * $CONFIG['nick']='zhuaxinlang4'; <br/>
	 * $CONFIG['sex']='m'; <br/>
	 * $CONFIG['watermark']='u/1982631825';
	 * 
	 * @param content
	 *            HTML source
	 * @return A HashMap with the config key and value pairs
	 */
	private static Map<String, String> extractConfig(String content) {
		Map<String, String> map = new HashMap<String, String>();
		for (String key : new String[] { "oid", "onick", "uid", "nick", "sex",
				"watermark" }) {
			int pos1 = content.indexOf("$CONFIG['" + key + "']=");
			int pos2 = content.indexOf("=", pos1) + 2;
			int pos3 = content.indexOf("'", pos2);
			if (pos3 > pos2) {
				String value = content.substring(pos2, pos3);
				map.put(key, value);
				if (DEBUG)
					System.out.println(key + ": " + value);
			}
		}
		return map;
	}

	/**
	 * Extract the fm objects (the data to be rendered) from the HTML source.
	 * Note: Only the fm objects with the key "html" are stored.
	 * 
	 * @param content
	 *            HTML source
	 * @return An ArrayList of parsed fm objects in JSONObject.
	 */
	private static List<JSONObject> extractFmObj(String content) {
		List<JSONObject> result = new ArrayList<JSONObject>();
		int pos1 = content.indexOf("<script>FM.view({");
		while (pos1 >= 0) {
			int pos2 = content.indexOf("{", pos1);
			int pos3 = content.indexOf("</script>", pos2);
			int pos4 = content.lastIndexOf("}", pos3) + 1; // Some ends with
															// "});</script>",
															// some ends with
															// "})</script>"
			String fmStr = content.substring(pos2, pos4);
			try {
				JSONObject fmObj = (JSONObject) JSONPARSER.parse(fmStr);
				if (fmObj.containsKey("html")) {
					result.add(fmObj);
				}
			} catch (ParseException e) {
				if (DEBUG)
					e.printStackTrace();
			}
			pos1 = content.indexOf("<script>FM.view({", pos3);
		}

		return result;
	}

	/**
	 * Extract the location header, including its header image, three counts and
	 * the full address.
	 * 
	 * @param html
	 *            The HTML source
	 * @return A WeiboLocation object with these information
	 */
	private static WeiboLocation extractLocationHeader(String html) {
		WeiboLocation wLocation = new WeiboLocation();
		Document doc = Jsoup.parse(html);
		// The image (head pic) of this location
		Elements divs = doc.getElementsByClass("pf_head_pic");
		if (divs.size() > 0) {
			String imgSrc = divs.get(0).getElementsByTag("img").get(0)
					.attr("src");
			wLocation.setHeadImg(imgSrc);
			if (DEBUG)
				System.out.println(imgSrc);
		}
		// The counts
		Elements tables = doc.getElementsByClass("W_tc");
		if (tables.size() > 0) {
			Map<String, String> map = new HashMap<String, String>();
			Elements tds = tables.get(0).getElementsByTag("td");
			for (Element td : tds) {
				String count = td.getElementsByTag("strong").get(0).text();
				String attr = td.getElementsByTag("span").get(0).text();
				map.put(attr, count);
				if (DEBUG)
					System.out.println(attr + ": " + count);
			}
			wLocation.setPicCount(new Long(map.get("热图")));
			wLocation.setLikeCount(new Long(map.get("赞")));
			wLocation.setDiscussCount(new Long(map.get("热议")));
		}
		// The address
		divs = doc.getElementsByClass("moreinfo");
		if (divs.size() > 0) {
			String address = divs.get(0).getElementsByClass("S_txt2").get(0)
					.text();
			address = address.substring(address.indexOf(":") + 1);
			if (DEBUG)
				System.out.println(address);
			wLocation.setFullAddress(address);
		}
		return wLocation;
	}

	/**
	 * Extract the nearby popular places.
	 * 
	 * @param html
	 *            The HTML source
	 * @return An ArrayList of WeiboLocation objects
	 */
	private static List<WeiboLocation> extractNearbyLocations(String html) {
		List<WeiboLocation> nearbyLocations = new ArrayList<WeiboLocation>();
		Document doc = Jsoup.parse(html);
		Elements lis = doc.getElementsByClass("pt_ul");
		if (lis != null && lis.size() > 0)
			for (Element li : doc.getElementsByClass("pt_ul").get(0)
					.getElementsByTag("li")) {
				WeiboLocation nLocation = new WeiboLocation();
				Element img = li.getElementsByTag("img").get(0);
				nLocation.setHeadImg(img.attr("src"));
				nLocation.setOnick(img.attr("title"));
				nLocation.setLink(li.getElementsByTag("h4").get(0)
						.getElementsByTag("a").attr("href"));
				for (Element sub : li.getElementsByClass("pt_sub").get(0)
						.getElementsByTag("span")) {
					String count = sub.getElementsByTag("a").text();
					String text = sub.text();
					if (text.contains("热议"))
						nLocation
								.setDiscussCount(NumberParser.parseLong(count));
					else if (text.contains("签到"))
						nLocation
								.setCheckinCount(NumberParser.parseLong(count));
					else if (text.contains("热图"))
						nLocation.setPicCount(NumberParser.parseLong(count));
				}
				nLocation.setFullAddress(li.getElementsByClass("pt_text")
						.text());
				nearbyLocations.add(nLocation);
			}
		return nearbyLocations;
	}

	/**
	 * Extract the nearby users full list (in the /checkin page, not the shorter
	 * one in the /home page)
	 * 
	 * @param html
	 *            The HTML source
	 * @return An ArrayList of WeiboUser objects
	 */
	private static List<WeiboUser> extractNearbyUsersFull(String html) {
		List<WeiboUser> nearbyUsers = new ArrayList<WeiboUser>();
		Document doc = Jsoup.parse(html);
		for (Element li : doc.getElementsByTag("li")) {
			WeiboUser wUser = new WeiboUser();

			// Basic info
			Element left = li.getElementsByClass("left").get(0);
			String link = left.getElementsByTag("a").attr("href");
			if (link.startsWith("/"))
				link = "http://weibo.com" + link;
			wUser.setLink(link);
			Element img = left.getElementsByTag("img").get(0);
			wUser.setNick(img.attr("alt"));
			wUser.setHeadImg(img.attr("src"));
			wUser.setUid(new Long(img.attr("usercard").substring(3)));

			// Verifications and VIPs
			Element name = li.getElementsByClass("name").get(0);
			Elements is = name.getElementsByTag("i");
			if (is != null && is.size() > 0) {
				Set<String> verifications = new HashSet<String>();
				for (Element i : is) {
					if (i.hasAttr("title")) {
						String title = i.attr("title");
						if (title.length() > 0)
							verifications.add(title);
					} else {
						if (i.attr("class").contains("ico_member"))
							verifications.add("微博会员");
					}
				}
				wUser.setVerifications(verifications);
			}

			// Address and gender
			Element addr = li.getElementsByClass("addr").get(0);
			String genderClass = addr.getElementsByTag("em").attr("class");
			if (genderClass.contains(" male"))
				wUser.setSex("m");
			else if (genderClass.contains(" female"))
				wUser.setSex("f");
			wUser.setAddress(addr.text());

			// Social connections count
			Element connect = li.getElementsByClass("connect").get(0);
			for (Element a : connect.getElementsByTag("a")) {
				String href = a.attr("href");
				String value = a.text();
				Long valueLong = NumberParser.parseLong(value);
				if (href.endsWith("/follow"))
					wUser.setFollowingCount(valueLong);
				else if (href.endsWith("/fans"))
					wUser.setFollowerCount(valueLong);
				else
					wUser.setMessageCount(valueLong);
			}

			Elements infos = li.getElementsByClass("info");
			if (infos != null && infos.size() > 0)
				wUser.setInfo(infos.text());

			Elements weibo = li.getElementsByClass("weibo").get(0)
					.getElementsByTag("a");
			WeiboMessage wMessage = new WeiboMessage();
			String mLink = weibo.attr("href");
			if (mLink.startsWith("/"))
				mLink = "http://weibo.com" + mLink;
			wMessage.setLink(mLink);
			wMessage.setText(weibo.html());
			wUser.setMessage(wMessage);

			nearbyUsers.add(wUser);
		}
		return nearbyUsers;
	}

	/**
	 * Extract the Weibo messages in the main feed
	 * 
	 * @param html
	 *            The HTML source
	 * @return An ArrayList of WeiboMessage object
	 */
	private static List<WeiboMessage> extractMessages(String html) {
		List<WeiboMessage> messages = new ArrayList<WeiboMessage>();
		Document doc = Jsoup.parse(html);

		for (Element div : doc.getElementsByAttributeValue("action-type",
				"feed_list_item")) {
			WeiboMessage wMessage = new WeiboMessage();

			// Basic info
			wMessage.setOuid(new Long(div.attr("tbinfo").substring(5)));
			wMessage.setMid(new Long(div.attr("mid")));
			Element e = div.getElementsByClass("WB_face").get(0);
			WeiboUser wmUser = new WeiboUser();
			String link = e.getElementsByTag("a").get(0).attr("href");
			if (link.startsWith("/"))
				link = "http://weibo.com" + link;
			wmUser.setLink(link);
			Element img = div.getElementsByTag("img").get(0);
			wmUser.setUid(new Long(img.attr("usercard").substring(3)));
			wmUser.setNick(img.attr("title"));
			wmUser.setHeadImg(img.attr("src"));
			Set<String> verifications = new HashSet<String>();
			for (Element verify : div.getElementsByClass("WB_info").get(0)
					.getElementsByTag("i")) {
				// Verified user
				if (verify.attr("title").length() > 0)
					verifications.add(verify.attr("title"));
			}
			wmUser.setVerifications(verifications);

			wMessage.setUser(wmUser);

			wMessage.setText(div.getElementsByClass("WB_text").get(0).html());

			// The embedded map
			Elements divs = div.getElementsByClass("map_data");
			if (divs != null && divs.size() > 0) {
				WeiboLocation wmLocation = new WeiboLocation();
				Element mapData = divs.get(0);
				String fullAddr = mapData.text();
				fullAddr = fullAddr.substring(0, fullAddr.indexOf(" - "));
				wmLocation.setFullAddress(fullAddr);
				String[] actionDatas = mapData.getElementsByTag("a").get(0)
						.attr("action-data").split("&");
				for (String actionData : actionDatas) {
					int pos = actionData.indexOf('=');
					String key = actionData.substring(0, pos);
					String value = actionData.substring(pos + 1);
					if (key.equals("geo"))
						wmLocation.setGeo(value);
					else if (key.equals("head"))
						wmLocation.setHeadImg(value);
				}
			}

			// Media (pictures, location details, etc.)
			Elements ul = div.getElementsByTag("ul");
			if (ul != null && ul.size() > 0) {
				List<String> medias = new ArrayList<String>();
				for (Element li : ul.get(0).getElementsByTag("li"))
					medias.add(li.html());
				wMessage.setMedias(medias);
			}

			// Post date and source
			divs = div.getElementsByClass("WB_from");
			if (divs != null && divs.size() > 0) {
				Elements as = divs.get(0).getElementsByTag("a");
				wMessage.setPostTime(as.get(0).attr("title"));
				String href = as.get(0).attr("href");
				if (href.startsWith("/"))
					href = "http://www.weibo.com" + href;
				wMessage.setLink(href);

				wMessage.setTimestamp(new Long(as.get(0).attr("date")));
				wMessage.setSourceLink(as.get(1).attr("href"));
				wMessage.setSourceText(as.get(1).text());
			}

			messages.add(wMessage);
		}
		return messages;
	}

	/**
	 * Get nearby popular locations of the current location.
	 * 
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public static WeiboLocation getNearby(double longitude, double latitude) {
		String content = WeiboLogin.getContent(new HttpGet(
				"http://www.weibo.com/p/100101" + longitude + "_" + latitude
						+ "/nearby"), "UTF-8");
		// try {
		// BufferedWriter bw = new BufferedWriter(new FileWriter(
		// "data/nearby.nearby.html"));
		// bw.write(content);
		// bw.newLine();
		// bw.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// String content = "";
		// try {
		// BufferedReader br = new BufferedReader(new FileReader(
		// "data/nearby.nearby.html"));
		// String line;
		// while ((line = br.readLine()) != null) {
		// content += line;
		// }
		// br.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		if (content == null)
			return null;

		WeiboLocation wLocation = new WeiboLocation();
		WeiboUser wUser = new WeiboUser();

		/**
		 * Read some meta-info of the user and location
		 */
		Map<String, String> map = extractConfig(content);
		wLocation.setOid(map.get("oid"));
		wLocation.setOnick(map.get("onick"));
		wUser.setUid(new Long(map.get("uid")));
		wUser.setNick(map.get("nick"));
		wUser.setSex(map.get("sex"));
		wUser.setWatermark(map.get("watermark"));
		wLocation.setUser(wUser);

		/**
		 * Now goes to the FM.view({}) sections
		 */
		for (JSONObject fmObj : extractFmObj(content)) {
			String ns = (String) fmObj.get("ns"), domid = (String) fmObj
					.get("domid"), html = (String) fmObj.get("html");
			if (domid.startsWith("Pl_Core_Header__")) {
				// Header meta-info
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				WeiboLocation tmpLocation = extractLocationHeader(html);
				wLocation.setHeadImg(tmpLocation.getHeadImg());
				wLocation.setPicCount(tmpLocation.getPicCount());
				wLocation.setLikeCount(tmpLocation.getLikeCount());
				wLocation.setDiscussCount(tmpLocation.getDiscussCount());
				wLocation.setFullAddress(tmpLocation.getFullAddress());
			} else if (domid.startsWith("Pl_Core_LeftTicketList__")) {
				// Nearby locations
				Document doc = Jsoup.parse(html);
				wLocation.setNearbyLocationCount(extractTotalCount(html));

				List<WeiboLocation> nearbyLocations = new ArrayList<WeiboLocation>();
				// Each location record
				Elements divs = doc.getElementsByClass("PRF_pictext_b");
				if (divs != null && divs.size() > 0) {
					for (Element div : divs) {
						WeiboLocation wmLocation = new WeiboLocation();

						Element a = div.getElementsByClass("pt_pic").get(0)
								.getElementsByTag("a").get(0);
						wmLocation.setLink(a.attr("href"));
						wmLocation.setHeadImg(a.getElementsByTag("img").attr(
								"src"));

						Element d = div.getElementsByClass("pt_title").get(0);
						wmLocation.setFullAddress(d.getElementsByTag("h4")
								.text());
						wmLocation.setOnick(d.getElementsByTag("a").get(0)
								.attr("title"));

						wmLocation.setCheckinCount(new Long(div
								.getElementsByClass("S_txt2").get(0)
								.getElementsByTag("a").text()));

						nearbyLocations.add(wmLocation);
					}
				}

				wLocation.setNearbyLocations(nearbyLocations);
			} else {
				if (DEBUG)
					System.out.println("Skipped: " + ns + ", " + domid);
			}
		}
		return wLocation;
	}

	/**
	 * Get related Weibo messages posted around this location.
	 * 
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public static WeiboLocation getRelateWeibo(double longitude, double latitude) {
		// String content = WeiboLogin.getContent(new HttpGet(
		// "http://www.weibo.com/p/100101" + longitude + "_" + latitude
		// + "/relateweibo"), "UTF-8");
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					"data/nearby.relateweibo.html"));
			String line;
			while ((line = br.readLine()) != null) {
				content += line;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (content == null)
			return null;

		WeiboLocation wLocation = new WeiboLocation();
		WeiboUser wUser = new WeiboUser();

		/**
		 * Read some meta-info of the user and location
		 */
		Map<String, String> map = extractConfig(content);
		wLocation.setOid(map.get("oid"));
		wLocation.setOnick(map.get("onick"));
		wUser.setUid(new Long(map.get("uid")));
		wUser.setNick(map.get("nick"));
		wUser.setSex(map.get("sex"));
		wUser.setWatermark(map.get("watermark"));
		wLocation.setUser(wUser);

		/**
		 * Now goes to the FM.view({}) sections
		 */
		for (JSONObject fmObj : extractFmObj(content)) {
			String ns = (String) fmObj.get("ns"), domid = (String) fmObj
					.get("domid"), html = (String) fmObj.get("html");
			if (domid.startsWith("Pl_Core_Header__")) {
				// Header meta-info
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				WeiboLocation tmpLocation = extractLocationHeader(html);
				wLocation.setHeadImg(tmpLocation.getHeadImg());
				wLocation.setPicCount(tmpLocation.getPicCount());
				wLocation.setLikeCount(tmpLocation.getLikeCount());
				wLocation.setDiscussCount(tmpLocation.getDiscussCount());
				wLocation.setFullAddress(tmpLocation.getFullAddress());
			} else if (domid.startsWith("Pl_Core_MixFeed__")) {
				// Nearby Weibo messages
				if (DEBUG)
					System.out.println(ns + ", " + domid);

				// 共xxx条
				Long totalCount = extractTotalCount(html);
				if (totalCount != null)
					wLocation.setMessageCount(totalCount);

				// The main feed
				wLocation.setMessages(extractMessages(html));
			} else if (domid.startsWith("Pl_Core_RightRank__")) {
				// Nearby popular places
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				wLocation.setNearbyLocations(extractNearbyLocations(html));
			} else {
				if (DEBUG)
					System.out.println("Skipped: " + ns + ", " + domid);
			}
		}
		return wLocation;
	}

	/**
	 * Get nearby pictures, not implemented because the strategy is different.
	 * 
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	@Deprecated
	public static WeiboLocation getAlbum(double longitude, double latitude) {
		String content = WeiboLogin.getContent(new HttpGet(
				"http://www.weibo.com/p/100101" + longitude + "_" + latitude
						+ "/album"), "UTF-8");
		// try {
		// BufferedWriter bw = new BufferedWriter(new FileWriter(
		// "data/nearby.album.html"));
		// bw.write(content);
		// bw.newLine();
		// bw.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// String content = "";
		// try {
		// BufferedReader br = new BufferedReader(new FileReader(
		// "data/nearby.album.html"));
		// String line;
		// while ((line = br.readLine()) != null) {
		// content += line;
		// }
		// br.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		if (content == null)
			return null;

		WeiboLocation wLocation = new WeiboLocation();
		WeiboUser wUser = new WeiboUser();

		/**
		 * Read some meta-info of the user and location
		 */
		Map<String, String> map = extractConfig(content);
		wLocation.setOid(map.get("oid"));
		wLocation.setOnick(map.get("onick"));
		wUser.setUid(new Long(map.get("uid")));
		wUser.setNick(map.get("nick"));
		wUser.setSex(map.get("sex"));
		wUser.setWatermark(map.get("watermark"));
		wLocation.setUser(wUser);

		// Then fetch the waterfall page
		content = WeiboLogin
				.getContent(
						new HttpGet(
								"http://photo.weibo.com/page/waterfall?filter=&page=1&count=20&module_id=poi_album&oid="
										+ wLocation.getOid()
										+ "&uid="
										+ wUser.getUid()
										+ "&lastMid=&lang=zh-cn&_t=1&callback=STK_"
										+ new Date().getTime() + "0"), "UTF-8");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					"data/nearby.waterfall.html"));
			bw.write(content);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/**
		 * Now goes to the FM.view({}) sections
		 */
		for (JSONObject fmObj : extractFmObj(content)) {
			String ns = (String) fmObj.get("ns"), domid = (String) fmObj
					.get("domid"), html = (String) fmObj.get("html");
			if (domid.startsWith("Pl_Core_Header__")) {
				// Header meta-info
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				WeiboLocation tmpLocation = extractLocationHeader(html);
				wLocation.setHeadImg(tmpLocation.getHeadImg());
				wLocation.setPicCount(tmpLocation.getPicCount());
				wLocation.setLikeCount(tmpLocation.getLikeCount());
				wLocation.setDiscussCount(tmpLocation.getDiscussCount());
				wLocation.setFullAddress(tmpLocation.getFullAddress());
			} else if (domid.startsWith("Pl_Third_Inline__")) {
				// Nearby photos
				Document doc = Jsoup.parse(html);
				System.out.println(doc);
			} else {
				if (DEBUG)
					System.out.println("Skipped: " + ns + ", " + domid);
			}
		}
		return wLocation;
	}

	/**
	 * Get the checkin page of a location: The users who have posted Weibo
	 * messages near here.
	 * 
	 * @param longitude
	 * @param latitude
	 * @return
	 */
	public static WeiboLocation getCheckin(double longitude, double latitude) {
		String content = WeiboLogin.getContent(new HttpGet(
				"http://www.weibo.com/p/100101" + longitude + "_" + latitude
						+ "/checkin"), "UTF-8");
		// String content = "";
		// try {
		// BufferedReader br = new BufferedReader(new FileReader(
		// "data/nearby.checkin.html"));
		// String line;
		// while ((line = br.readLine()) != null) {
		// content += line;
		// }
		// br.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		if (content == null)
			return null;

		WeiboLocation wLocation = new WeiboLocation();
		WeiboUser wUser = new WeiboUser();

		/**
		 * Read some meta-info of the user and location
		 */
		Map<String, String> map = extractConfig(content);
		wLocation.setOid(map.get("oid"));
		wLocation.setOnick(map.get("onick"));
		wUser.setUid(new Long(map.get("uid")));
		wUser.setNick(map.get("nick"));
		wUser.setSex(map.get("sex"));
		wUser.setWatermark(map.get("watermark"));
		wLocation.setUser(wUser);

		/**
		 * Now goes to the FM.view({}) sections
		 */
		for (JSONObject fmObj : extractFmObj(content)) {
			String ns = (String) fmObj.get("ns"), domid = (String) fmObj
					.get("domid"), html = (String) fmObj.get("html");
			if (domid.startsWith("Pl_Core_Header__")) {
				// Header meta-info
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				WeiboLocation tmpLocation = extractLocationHeader(html);
				wLocation.setHeadImg(tmpLocation.getHeadImg());
				wLocation.setPicCount(tmpLocation.getPicCount());
				wLocation.setLikeCount(tmpLocation.getLikeCount());
				wLocation.setDiscussCount(tmpLocation.getDiscussCount());
				wLocation.setFullAddress(tmpLocation.getFullAddress());
			} else if (domid.startsWith("Pl_Core_LeftUserList__")) {
				// Nearby users full list
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				wLocation.setNearbyUsers(extractNearbyUsersFull(html));
			} else if (domid.startsWith("Pl_Core_RightRank__")) {
				// Nearby popular places
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				wLocation.setNearbyLocations(extractNearbyLocations(html));
			} else {
				if (DEBUG)
					System.out.println("Skipped: " + ns + ", " + domid);
			}
		}
		return wLocation;
	}

	/**
	 * Get the home page of a location, containing related locations, messages,
	 * thumbnail pictures, checkins etc.
	 * 
	 * @param longitude
	 *            The longitude of the location in degree, east is positive,
	 *            west is negative.
	 * @param latitude
	 *            The latitude of the location in degree, north is positive,
	 *            south is negative.
	 * @return A WeiboLocation object.
	 */
	public static WeiboLocation getHome(double longitude, double latitude) {
		String content = WeiboLogin.getContent(new HttpGet(
				"http://www.weibo.com/p/100101" + longitude + "_" + latitude
						+ "/home"), "UTF-8");
		// BufferedReader br = new BufferedReader(new FileReader(
		// "data/nearby.home.1.html"));
		// String content = "";
		// String line;
		// while ((line = br.readLine()) != null) {
		// content += line;
		// }
		// br.close();

		if (content == null)
			return null;

		WeiboLocation wLocation = new WeiboLocation();
		WeiboUser wUser = new WeiboUser();

		/**
		 * Read some meta-info of the user and location
		 */
		Map<String, String> map = extractConfig(content);
		wLocation.setOid(map.get("oid"));
		wLocation.setOnick(map.get("onick"));
		wUser.setUid(new Long(map.get("uid")));
		wUser.setNick(map.get("nick"));
		wUser.setSex(map.get("sex"));
		wUser.setWatermark(map.get("watermark"));
		wLocation.setUser(wUser);

		/**
		 * Now goes to the FM.view({}) sections
		 */
		for (JSONObject fmObj : extractFmObj(content)) {
			/**
			 * The "ns" and "domid" keys in this object might be one of the
			 * followings (note the number in the end may change):
			 */
			/*
			 * Useful:
			 */
			// Header meta-info of the location:
			// "ns":"pl.header.head.index","domid":"Pl_Core_Header__1"
			// Nearby popular pictures:
			// "ns":"pl.content.album.index","domid":"Pl_Core_LeftPic__8"
			// Main feed of Weibo messages:
			// "ns":"pl.content.homeFeed.index","domid":"Pl_Core_MixFeed__14"
			// Nearby users: "ns":"","domid":"Pl_Core_RightUserGrid__21"
			// Nearby popular places:
			// "ns":"","domid":"Pl_Core_RightRank__24"

			/*
			 * No need:
			 */
			// User navigation bar:
			// "ns":"pl.top.index","domid":"pl_common_top"
			// Footer:
			// "ns":"pl.content.changeLanguage.index","domid":"pl_common_footer"
			// Location page navigation bar:
			// "ns":"pl.nav.index","domid":"Pl_Core_Nav__2"
			// Empty: "ns":"","domid":"plc_main"
			// Friends tracks (Zuji):
			// "ns":"trustPagelet.pageRight.lbs.fzj","domid":"Pl_Third_Inline__22"
			// Coupons:
			// "ns":"trustPagelet.pageRight.lbs.yhq","domid":"Pl_Third_Inline__23"
			// Empty:
			// "ns":"pl.content.homeFeed.index","domid":"Pl_Core_OwnerFeed__5"
			// Map:
			// "ns":"pl.right.map.index","domid":"Pl_Core_RightMap__18"
			// Post instruction:
			// "ns":"","domid":"Pl_Core_RightTextSingleLite__25"
			String ns = (String) fmObj.get("ns"), domid = (String) fmObj
					.get("domid"), html = (String) fmObj.get("html");
			if (domid.startsWith("Pl_Core_Header__")) {
				// Header meta-info
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				WeiboLocation tmpLocation = extractLocationHeader(html);
				wLocation.setHeadImg(tmpLocation.getHeadImg());
				wLocation.setPicCount(tmpLocation.getPicCount());
				wLocation.setLikeCount(tmpLocation.getLikeCount());
				wLocation.setDiscussCount(tmpLocation.getDiscussCount());
				wLocation.setFullAddress(tmpLocation.getFullAddress());
			} else if (domid.startsWith("Pl_Core_LeftPic__")) {
				// Nearby pictures
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				List<WeiboMessage> pictures = new ArrayList<WeiboMessage>();
				Document doc = Jsoup.parse(html);
				for (Element li : doc.getElementsByClass("picitems")) {
					WeiboMessage wMessage = new WeiboMessage();
					Element a = li.getElementsByTag("a").get(0);
					String actionData = a.attr("action-data");
					String url = actionData.substring(
							actionData.indexOf("&url=") + 5,
							actionData.indexOf("&url_name="));
					wMessage.setLink(url);
					if (DEBUG)
						System.out.println(url);
					Element img = a.getElementsByTag("img").get(0);
					String imgSrc = img.attr("src");
					String title = img.attr("title");
					wMessage.setPhoto(imgSrc);
					if (DEBUG)
						System.out.println(imgSrc);
					wMessage.setText(title);
					if (DEBUG)
						System.out.println(title);
					pictures.add(wMessage);
				}
				wLocation.setPictures(pictures);
			} else if (domid.startsWith("Pl_Core_MixFeed__")) {
				// Nearby Weibo messages
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				wLocation.setMessages(extractMessages(html));
			} else if (domid.startsWith("Pl_Core_RightUserGrid__")) {
				// Nearby users
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				List<WeiboUser> nearbyUsers = new ArrayList<WeiboUser>();
				Document doc = Jsoup.parse(html);
				String nearbyCount = doc.getElementsByTag("legend").text();
				nearbyCount = nearbyCount.substring(0,
						nearbyCount.indexOf("人路过这里"));
				wLocation.setNearbyUserCount(new Long(nearbyCount));
				for (Element li : doc.getElementsByTag("li")) {
					WeiboUser nUser = new WeiboUser();
					String link = li.getElementsByTag("a").get(0).attr("href");
					nUser.setLink(link);
					Element img = li.getElementsByTag("img").get(0);
					nUser.setHeadImg(img.attr("src"));
					nUser.setNick(img.attr("title"));
					nUser.setUid(new Long(img.attr("usercard").substring(3)));
					nearbyUsers.add(nUser);
				}
				wLocation.setNearbyUsers(nearbyUsers);
			} else if (domid.startsWith("Pl_Core_RightRank__")) {
				// Nearby popular places
				if (DEBUG)
					System.out.println(ns + ", " + domid);
				wLocation.setNearbyLocations(extractNearbyLocations(html));
			} else {
				if (DEBUG)
					System.out.println("Skipped: " + ns + ", " + domid);
			}
		}

		return wLocation;
	}

	public static void main(String[] args) {
		if (WeiboLogin.updateCookie("username", "password")) {
			/*
			 * Wangfujing
			 */
			double[] geoLocation = new double[] { 116.411126, 39.913456 };

			/*
			 * Wudaokou
			 */
			// double[] geoLocation = new double[] { 116.339071, 39.992476 };

			/*
			 * Taihu lake: Empty content
			 */
			// double[] geoLocation = new double[] { 120.146255, 31.249204 };

			/*
			 * Davis Center, Waterloo, ON, Canada
			 */
			// double[] geoLocation = new double[] { -80.542189, 43.472675 };

			// System.out.println("Home:\n"
			// + WeiboNearbyParser.getHome(geoLocation[0], geoLocation[1]));
			// System.out.println("Checkin:\n"
			// + WeiboNearbyParser.getCheckin(geoLocation[0], geoLocation[1]));
			// System.out.println("Album:\n"
			// + WeiboNearbyParser.getAlbum(geoLocation[0], geoLocation[1]));
			System.out.println("RelateWeibo:\n"
					+ WeiboNearbyParser.getRelateWeibo(geoLocation[0],
							geoLocation[1]));
			// System.out.println("Nearby:\n"
			// + WeiboNearbyParser.getNearby(geoLocation[0], geoLocation[1]));
		}
	}
}
