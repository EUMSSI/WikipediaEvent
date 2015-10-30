package de.l3s.eumssi.model;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class Reference {
	private String id;
	private String url;
	private String type;
	private String source;
	
	
	public Reference(){
		id = null;
		url = null;
		type = "article";
		source = null;
	}
	
	public Reference(String rid, String rurl, String rsource) {
		id = rid;
		url = rurl;
		type = "article";
		source = rsource;
	}
	
	public void setUrl(String rurl) {
		url = rurl;
		
//		String [] tmp = url.split("/");
//		for (int i = 0; i<tmp.length; i++) {
//			if (tmp[i].contains("http")|| tmp[i].length() ==0) continue;
//			source = tmp[i];
//			break;
//		}
	}
	
	public void setSource(String sourcename){
		source = sourcename;
	}
	public void setType(String t) {
		type = t;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public String getType() {
		return type;
	}

	public String getSource() {
		return source;
	}
	
	
	@Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        Reference otherReference = (Reference) other;
        try{        	
        	return otherReference.getId().equals(this.getId());
        }catch(Exception e){
        	e.printStackTrace();
        	return false;
        }
    }
	
	
}
