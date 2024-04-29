import com.mrpowergamerbr.temmiewebhook.*;
import com.mrpowergamerbr.temmiewebhook.embed.*;
import java.util.*;

public class Discord
{
    private String color;
    private String footer;
    private String footerIcon;
    
    public Discord(final String color, final String footer, final String footerIcon) {
        this.color = color;
        this.footer = footer;
        this.footerIcon = footerIcon;
    }
    
    public void webHookMessage(final String username, final String hook, final String link, final boolean isVideo, final String video, final String image, final String profilePic) {
        final TemmieWebhook temmie = new TemmieWebhook(hook);
        final FieldEmbed fe = new FieldEmbed();
        final FieldEmbed isVid = new FieldEmbed();
        if (!isVideo) {
            final ImageEmbed ie = new ImageEmbed();
            ie.setUrl(image);
        }
        else {
            final VideoEmbed ve = new VideoEmbed();
            ve.setUrl(video);
        }
        fe.setName("Story Link");
        if (!link.equals("")) {
            fe.setValue(link);
        }
        else {
            fe.setValue("None found");
        }
        isVid.setName("Is Video?");
        isVid.setValue(new StringBuilder().append(isVideo).toString());
        final ImageEmbed ie = new ImageEmbed();
        ie.setUrl(image);
        if (isVideo) {
            final DiscordMessage dm = DiscordMessage.builder().username("insta-story-monitor").content(video).avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").build();
            temmie.sendMessage(dm);
        }
        final DiscordEmbed de = DiscordEmbed.builder().author(AuthorEmbed.builder().name(username).url("https://instagram.com/" + username).icon_url(profilePic).build()).title(String.valueOf(username) + " - Instagram Story").field(isVid).image(ie).url("https://instagram.com/stories/" + username).field(fe).footer(FooterEmbed.builder().text(this.footer).icon_url(this.footerIcon).build()).build();
        de.setColor(Integer.parseInt(this.color, 16));
        final DiscordMessage dm2 = DiscordMessage.builder().username("insta-story-monitor").content("").avatarUrl("https://diylogodesigns.com/wp-content/uploads/2016/05/instagram-Logo-PNG-Transparent-Background-download-768x768.png").embeds(Arrays.asList(de)).build();
        temmie.sendMessage(dm2);
    }
}
