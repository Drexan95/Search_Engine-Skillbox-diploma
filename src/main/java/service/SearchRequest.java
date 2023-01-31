package service;


public class SearchRequest {
    private final String text;
    private final String siteUrl;
    private final Integer limit;
    private final Integer offset;

    public SearchRequest(String text, String siteUrl,Integer limit, Integer offset) {
        this.limit = limit;
        this.siteUrl = siteUrl;
        this.text = text;
        this.offset = offset;
    }
    public String getText() {
        return text;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

}
   class SearchRequestBuilder{
        private String text = "";
        private String siteUrl = "";
        private Integer limit = 0;
        private  Integer offset = 0;

        SearchRequestBuilder setText(String text){
            this.text = text;
            return this;
        }
        SearchRequestBuilder setSiteUrl(String siteUrl){
            if(siteUrl != null) {
                this.siteUrl = siteUrl;
            }
            return this;
        }
        SearchRequestBuilder setLimit(Integer limit){
            if(limit!=null) {
                this.limit = limit;
            }
            return this;
        }
        SearchRequestBuilder setOffset(Integer offset){
            if(offset!=null) {
                this.offset = offset;
            }
            return this;
        }
        SearchRequest build(){
            return new SearchRequest(text, siteUrl,limit,offset);
        }
    }

