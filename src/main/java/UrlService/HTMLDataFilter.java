package UrlService;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class HTMLDataFilter {
    private static final List<String> UNSUPPORTED_TYPES = Arrays.asList("jpg", "pdf", "png", "gif", "zip",
            "tar", "jar", "gz", "svg", "ppt", "pptx");

    @Value("${fieldweight}")
    private float weights[];

    private static float[] fieldWeights;

    @Value("${fieldweight}")
    public void setWeights(float weights[]){
        HTMLDataFilter.fieldWeights = weights;
    }

    public static String findText(String text) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("[а-яёА-ЯЁ]+");
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            result.append(" ").append(m.group());
        }
        return result.toString();
    }

    protected   static  boolean skip(String url,String root)
    {
        if (url.contains("#") || url.contains("?")||url.trim().length() == 0||url.equalsIgnoreCase(root)||!isSupportedType(url)) {
           return true;
        }
        return false;
    }

    protected static boolean isInternalLink(String ulr,String path)
    {
        return ulr.startsWith(path);
    }

    protected static float calculateFieldWeight(Element element){
        String fieldTag = element.tagName();
        float weight;

        switch (fieldTag){
            case("head") :
                weight = fieldWeights[0];
            break;
            case ("body"): weight = fieldWeights[1];
            break;
            case ("div"): weight = fieldWeights[2];
            break;
            default:weight = fieldWeights[3];
            break;
        }
        return weight;
    }

    protected static boolean isSupportedType(String url) {

        if (url == null) {
            return false;
        }
        if (url.isEmpty()) {
            return true;
        }
        String ext = url.substring(url.lastIndexOf(".") + 1);
        if (ext == null) {
            return true;
        }
        return (!UNSUPPORTED_TYPES.contains(ext));
    }

   protected static boolean elementIsRedundant(Element element,String text) {

        if (text == null || text.equals("") ){
            return true;
        }

        if (element.hasAttr("*")) {
            return false;
        }
        if (element.hasText()) {
            return false;
        }
        if (element.childNodeSize() == 0 || element.childNodes().isEmpty()) {
            return true;
        }
        List<org.jsoup.nodes.Node> children = element.childNodes();
        for (Node child : children) {
            if (!child.toString().isEmpty() && !child.toString().matches("\\s*")) {
                return false;
            }
        }

        return true;
   }


    protected static  String slashAtEnd(String siteUrl){
        if(!siteUrl.endsWith("/")){
            siteUrl = siteUrl+ "/";
        }
        return siteUrl;
    }
}
