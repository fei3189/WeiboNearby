package info.caq9.basic;

import java.util.List;

import org.json.JSONObject;

public class WeiboMessage {
	String link, photo, text, postTime, sourceLink, sourceText;
	Long ouid, mid, timestamp;
	WeiboUser user;
	WeiboLocation location;
	List<String> medias;

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getPhoto() {
		return photo;
	}

	public void setPhoto(String photo) {
		this.photo = photo;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getPostTime() {
		return postTime;
	}

	public void setPostTime(String postTime) {
		this.postTime = postTime;
	}

	public String getSourceLink() {
		return sourceLink;
	}

	public void setSourceLink(String sourceLink) {
		this.sourceLink = sourceLink;
	}

	public String getSourceText() {
		return sourceText;
	}

	public void setSourceText(String sourceText) {
		this.sourceText = sourceText;
	}

	public Long getOuid() {
		return ouid;
	}

	public void setOuid(long ouid) {
		this.ouid = ouid;
	}

	public Long getMid() {
		return mid;
	}

	public void setMid(long mid) {
		this.mid = mid;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public WeiboUser getUser() {
		return user;
	}

	public void setUser(WeiboUser user) {
		this.user = user;
	}

	public WeiboLocation getLocation() {
		return location;
	}

	public void setLocation(WeiboLocation location) {
		this.location = location;
	}

	public List<String> getMedias() {
		return medias;
	}

	public void setMedias(List<String> medias) {
		this.medias = medias;
	}

	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
}
