package info.caq9.basic;

import java.util.Set;

import org.json.JSONObject;

public class WeiboUser {
	String nick, sex, watermark, link, headImg, address, info;
	Long uid, followingCount, followerCount, messageCount;
	Set<String> verifications;
	WeiboMessage message;

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public String getWatermark() {
		return watermark;
	}

	public void setWatermark(String watermark) {
		this.watermark = watermark;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getHeadImg() {
		return headImg;
	}

	public void setHeadImg(String headImg) {
		this.headImg = headImg;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public Long getUid() {
		return uid;
	}

	public void setUid(Long uid) {
		this.uid = uid;
	}

	public Long getFollowingCount() {
		return followingCount;
	}

	public void setFollowingCount(Long followingCount) {
		this.followingCount = followingCount;
	}

	public Long getFollowerCount() {
		return followerCount;
	}

	public void setFollowerCount(Long followerCount) {
		this.followerCount = followerCount;
	}

	public Long getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(Long messageCount) {
		this.messageCount = messageCount;
	}

	public Set<String> getVerifications() {
		return verifications;
	}

	public void setVerifications(Set<String> verifications) {
		this.verifications = verifications;
	}

	public WeiboMessage getMessage() {
		return message;
	}

	public void setMessage(WeiboMessage message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
}
