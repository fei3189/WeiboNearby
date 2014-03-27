package info.caq9.basic;

import java.util.List;

import org.json.JSONObject;

public class WeiboLocation {
	/**
	 * oid: Location longitude and latitude, concatenated with an underline.<br/>
	 * onick: Location's short name.<br/>
	 * headImg: Location head image.
	 */
	String oid, onick, headImg;

	/**
	 * fullAddress: The full address (the name) of this location.<br/>
	 * geo: The geo data (latitude and longitude) of this location.<br/>
	 * link: The web url of visiting this location page.
	 */
	String fullAddress, geo, link;

	/**
	 * The count of several related staff...
	 */
	Long picCount, likeCount, discussCount, nearbyUserCount, checkinCount,
			messageCount, nearbyLocationCount;

	/**
	 * The user information who is used to fetch (log in) this page.
	 */
	WeiboUser user;

	/**
	 * The pictures related to this location. It is in HTML format -- Not parsed
	 * yet.
	 */
	List<WeiboMessage> pictures;

	/**
	 * The Weibo messages related to this location.
	 */
	List<WeiboMessage> messages;

	/**
	 * Nearby users.
	 */
	List<WeiboUser> nearbyUsers;

	/**
	 * Nearby popular locations.
	 */
	List<WeiboLocation> nearbyLocations;

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getOnick() {
		return onick;
	}

	public void setOnick(String onick) {
		this.onick = onick;
	}

	public String getHeadImg() {
		return headImg;
	}

	public void setHeadImg(String headImg) {
		this.headImg = headImg;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public void setFullAddress(String fullAddress) {
		this.fullAddress = fullAddress;
	}

	public String getGeo() {
		return geo;
	}

	public void setGeo(String geo) {
		this.geo = geo;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Long getPicCount() {
		return picCount;
	}

	public void setPicCount(Long picCount) {
		this.picCount = picCount;
	}

	public Long getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(Long likeCount) {
		this.likeCount = likeCount;
	}

	public Long getDiscussCount() {
		return discussCount;
	}

	public void setDiscussCount(Long discussCount) {
		this.discussCount = discussCount;
	}

	public Long getNearbyUserCount() {
		return nearbyUserCount;
	}

	public void setNearbyUserCount(Long nearbyUserCount) {
		this.nearbyUserCount = nearbyUserCount;
	}

	public Long getCheckinCount() {
		return checkinCount;
	}

	public void setCheckinCount(Long checkinCount) {
		this.checkinCount = checkinCount;
	}

	public Long getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(Long messageCount) {
		this.messageCount = messageCount;
	}

	public Long getNearbyLocationCount() {
		return nearbyLocationCount;
	}

	public void setNearbyLocationCount(Long nearbyLocationCount) {
		this.nearbyLocationCount = nearbyLocationCount;
	}

	public WeiboUser getUser() {
		return user;
	}

	public void setUser(WeiboUser user) {
		this.user = user;
	}

	public List<WeiboMessage> getPictures() {
		return pictures;
	}

	public void setPictures(List<WeiboMessage> pictures) {
		this.pictures = pictures;
	}

	public List<WeiboMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<WeiboMessage> messages) {
		this.messages = messages;
	}

	public List<WeiboUser> getNearbyUsers() {
		return nearbyUsers;
	}

	public void setNearbyUsers(List<WeiboUser> nearbyUsers) {
		this.nearbyUsers = nearbyUsers;
	}

	public List<WeiboLocation> getNearbyLocations() {
		return nearbyLocations;
	}

	public void setNearbyLocations(List<WeiboLocation> nearbyLocations) {
		this.nearbyLocations = nearbyLocations;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
}
