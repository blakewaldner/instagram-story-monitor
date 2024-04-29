import org.json.simple.*;
import java.io.*;
import org.json.simple.parser.*;
import java.text.*;
import java.util.*;
import org.jsoup.*;
import java.time.*;
import java.net.*;

public class Monitor implements Runnable
{
    private final ArrayList<String> proxies;
    private final ArrayList<String> accounts;
    private HashMap<String, String[]> hookEmbeds;
    private HashMap<String, ArrayList<String>> accountHooks;
    private int delay;
    private String session;
    private int attempts;
    
    public Monitor() throws IOException, ParseException {
        ThreadLocalAuthenticator.setAsDefault();
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", "");
        final Scanner scanProx = new Scanner(new File("proxies.txt"));
        this.proxies = new ArrayList<String>();
        while (scanProx.hasNextLine()) {
            this.proxies.add(scanProx.nextLine());
        }
        scanProx.close();
        final JSONParser parser = new JSONParser();
        final FileReader configFile = new FileReader("config.txt");
        final JSONObject config = (JSONObject)parser.parse(configFile);
        this.delay = Integer.parseInt(config.get("delay"));
        this.session = config.get("session");
        final FileReader embedFile = new FileReader("embed.txt");
        final JSONObject embed = (JSONObject)parser.parse(embedFile);
        final int webhooksAmt = embed.size();
        final String[][] embeds = new String[webhooksAmt][3];
        for (int x = 0; x < embed.size(); ++x) {
            final JSONArray embedArray = embed.get(new StringBuilder().append(x + 1).toString());
            embeds[x][0] = embedArray.get(0);
            embeds[x][1] = embedArray.get(1);
            embeds[x][2] = embedArray.get(2);
        }
        this.accounts = new ArrayList<String>();
        this.hookEmbeds = new HashMap<String, String[]>();
        this.accountHooks = new HashMap<String, ArrayList<String>>();
        for (int x = 0; x < webhooksAmt; ++x) {
            final Scanner scanAccounts = new Scanner(new File(String.valueOf(x + 1) + ".txt"));
            final String hook = scanAccounts.nextLine();
            this.hookEmbeds.put(hook, embeds[x]);
            while (scanAccounts.hasNextLine()) {
                final String account = scanAccounts.nextLine();
                if (!this.accounts.contains(account)) {
                    this.accounts.add(account);
                    this.accountHooks.put(account, new ArrayList<String>());
                }
                this.accountHooks.get(account).add(hook);
            }
            scanAccounts.close();
        }
        this.attempts = 0;
        System.out.println(String.valueOf(this.proxies.size()) + " proxies loaded");
    }
    
    public void run() {
        final Random r = new Random();
        for (final String account : this.accounts) {
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    final MessageFormat f = new MessageFormat("https://www.instagram.com/graphql/query/?query_hash=45246d3fe16ccc6577e0bd297a5db1ab&variables=%7B%22reel_ids%22%3A%5B%22{0}%22%5D%2C%22tag_names%22%3A%5B%5D%2C%22location_ids%22%3A%5B%5D%2C%22highlight_reel_ids%22%3A%5B%5D%2C%22precomposed_overlay%22%3Afalse%7D");
                    Proxy currentProxy = null;
                    int currentProxyIdx = r.nextInt(Monitor.this.proxies.size());
                    final String url = "https://instagram.com/" + account + "?__a=1";
                    String id = "";
                    String currentStoryID = "";
                    String profilePic = "";
                    boolean gotInitialID = false;
                    while (!gotInitialID) {
                        try {
                            ++currentProxyIdx;
                            currentProxy = Monitor.this.rotateProxy(currentProxy, currentProxyIdx);
                            final JSONObject user = Monitor.this.getUser(Monitor.this.getStories(url, currentProxy).body());
                            id = user.get("id").toString();
                            profilePic = user.get("profile_pic_url_hd").toString();
                            gotInitialID = true;
                            System.out.println("Got initial userid for " + account);
                        }
                        catch (ParseException ex) {}
                        catch (IOException e5) {
                            System.out.println("Caught ioexception on getting userid, retrying initial userid");
                            gotInitialID = false;
                            try {
                                Thread.sleep(Monitor.this.delay);
                            }
                            catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    final String storiesUrl = f.format(new Object[] { id });
                    boolean gotInitialStoryID = false;
                    while (!gotInitialStoryID) {
                        try {
                            currentStoryID = Monitor.this.getStory(Monitor.this.getStories(storiesUrl, currentProxy).body()).get("id").toString();
                            gotInitialStoryID = true;
                            System.out.println("Got initial story id for " + account + " -> proceeding to monitor");
                        }
                        catch (ParseException ex2) {}
                        catch (IndexOutOfBoundsException e6) {
                            System.out.println("Caught no stories");
                            gotInitialStoryID = true;
                            System.out.println("Got initial story id for " + account + " -> proceeding to monitor");
                        }
                        catch (IOException e7) {
                            ++currentProxyIdx;
                            currentProxy = Monitor.this.rotateProxy(currentProxy, currentProxyIdx);
                            System.out.println("Caught ioexception on getting story id, retrying initial story id");
                            gotInitialStoryID = false;
                            try {
                                Thread.sleep(Monitor.this.delay);
                            }
                            catch (InterruptedException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                    while (true) {
                        final Monitor this$0 = Monitor.this;
                        Monitor.access$3(this$0, this$0.attempts + 1);
                        try {
                            ++currentProxyIdx;
                            currentProxy = Monitor.this.rotateProxy(currentProxy, currentProxyIdx);
                            final JSONObject story = Monitor.this.getStory(Monitor.this.getStories(storiesUrl, currentProxy).body());
                            currentStoryID = Monitor.this.checkStories(story, account, profilePic, currentStoryID);
                            System.out.println("Attempt " + Monitor.this.attempts + " proxy " + currentProxy.toString() + " to " + account + " stories" + " .. wait " + Monitor.this.delay + "ms");
                        }
                        catch (UncheckedIOException e3) {
                            System.out.println("UNCHECKED IO MF EXCEPTION (" + account + "): " + e3);
                        }
                        catch (NullPointerException e8) {
                            System.out.println("Caught no stories on " + account);
                        }
                        catch (IndexOutOfBoundsException e6) {
                            System.out.println("Caught no stories on " + account);
                        }
                        catch (IOException e7) {
                            System.out.println("IOException (" + account + "), likely rate limited on this proxy: " + currentProxy.toString());
                        }
                        catch (ParseException e4) {
                            System.out.println(e4 + "How the fuck did you get a parse exception");
                        }
                        try {
                            Thread.sleep(Monitor.this.delay);
                        }
                        catch (InterruptedException ex3) {}
                    }
                }
            };
            thread.start();
            System.out.println("Started thread for " + account + " thread id: " + thread.getId());
        }
    }
    
    public Connection.Response getStories(final String storiesUrl, final Proxy currentProxy) throws IOException {
        return Jsoup.connect(storiesUrl).proxy(currentProxy).followRedirects(true).ignoreContentType(true).timeout(20000).cookie("sessionid", this.session).method(Connection.Method.GET).execute();
    }
    
    public JSONObject getStory(final String body) throws ParseException {
        final JSONParser parser = new JSONParser();
        final int size = ((JSONObject)parser.parse(body)).get("data").get("reels_media").get(0).get("items").size();
        return ((JSONObject)parser.parse(body)).get("data").get("reels_media").get(0).get("items").get(size - 1);
    }
    
    public JSONObject getUser(final String body) throws ParseException {
        final JSONParser parser = new JSONParser();
        return ((JSONObject)parser.parse(body)).get("graphql").get("user");
    }
    
    public String checkStories(final JSONObject story, final String account, final String profilePic, final String currentStoryID) {
        boolean isVideo = false;
        String image = "";
        String video = "";
        String link = "";
        String storyID = "";
        storyID = story.get("id").toString();
        if (!storyID.equals(currentStoryID)) {
            final long timeStamp = story.get("taken_at_timestamp");
            long timeNow = Instant.now().toEpochMilli();
            timeNow /= 1000L;
            if (timeNow - timeStamp > 60L) {
                System.out.println("Old post!!!");
                return storyID;
            }
            System.out.println("Found a story");
            this.attempts = 0;
            try {
                link = story.get("story_cta_url").toString();
            }
            catch (NullPointerException e) {
                System.out.println("No link");
            }
            image = story.get("display_url").toString();
            isVideo = Boolean.parseBoolean(story.get("is_video").toString());
            if (isVideo) {
                final JSONArray video_resources = story.get("video_resources");
                final JSONObject videoObject = video_resources.get(0);
                video = videoObject.get("src").toString();
            }
            this.postDiscord(account, link, isVideo, video, image, profilePic);
        }
        return storyID;
    }
    
    public void postDiscord(final String account, final String link, final boolean isVideo, final String video, final String pic, final String profilePic) {
        final ArrayList<String> hooks = this.accountHooks.get(account);
        for (final String hook : hooks) {
            final String[] embeds = this.hookEmbeds.get(hook);
            final Discord d = new Discord(embeds[0], embeds[1], embeds[2]);
            d.webHookMessage(account, hook, link, isVideo, video, pic, profilePic);
        }
    }
    
    public Proxy rotateProxy(final Proxy currentProxy, final int currentProxyIdx) {
        final String selectedProxy = this.proxies.get(currentProxyIdx % this.proxies.size());
        final String[] proxyArr = selectedProxy.split(":");
        if (proxyArr.length == 2) {
            final String host = proxyArr[0];
            final int port = Integer.parseInt(proxyArr[1]);
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        }
        final String host = proxyArr[0];
        final int port = Integer.parseInt(proxyArr[1]);
        final String username = proxyArr[2];
        final String password = proxyArr[3];
        ThreadLocalAuthenticator.setProxyAuth(username, password);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }
    
    static /* synthetic */ void access$3(final Monitor monitor, final int attempts) {
        monitor.attempts = attempts;
    }
}