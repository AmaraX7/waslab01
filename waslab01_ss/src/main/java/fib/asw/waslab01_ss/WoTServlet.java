package fib.asw.waslab01_ss;

import java.io.*;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(value = "/")
public class WoTServlet extends HttpServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private TweetDAO tweetDAO;
    private final Locale currentLocale = Locale.forLanguageTag("en");
    private static final String SECRET_SALT = "asw_lab_1";

    public void init() {
        tweetDAO = new TweetDAO((java.sql.Connection) this.getServletContext().getAttribute("connection"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        List<Tweet> tweets = tweetDAO.getAllTweets();

        String req = request.getHeader("Accept");

        if (req.equals("text/plain")) {
            printPLAINresult(response, tweets);
        } else {
            printHTMLresults(response, tweets, request);
        }


    }
    private String generateHash(long tweetId) {
        try {
            String data = tweetId + SECRET_SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(data.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean canDeleteTweet(HttpServletRequest request, long tweetId) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        String expectedHash = generateHash(tweetId);
        String cookieName = "tweet_" + tweetId;

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName) && cookie.getValue().equals(expectedHash)) {
                return true;
            }
        }
        return false;
    }
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String tweet_param = request.getParameter("tweet_id");

        if (tweet_param != null) {
            try {
                int tweet_id = Integer.parseInt(tweet_param);

                if (canDeleteTweet(request, tweet_id)) {
                    tweetDAO.deleteTweet(tweet_id);

                    Cookie cookie = new Cookie("tweet_" + tweet_id, "");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);

                    String req = request.getHeader("Accept");

                    if ("text/plain".equals(req)) {
                        response.setContentType("text/plain");;
                        PrintWriter out = response.getWriter();
                        out.print("Tweet " + tweet_id + " deleted");

                    } else {
                        response.sendRedirect(request.getContextPath());
                    }
                } else {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "You cannot delete this tweet");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tweet ID");
                return;
            }
        }
        else {
            String author = request.getParameter("author");
            String tweetText = request.getParameter("tweet_text");

            long tweetId = 0;
            try {
                tweetId = tweetDAO.insertTweet(author, tweetText);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            String hash = generateHash(tweetId);
            Cookie cookie = new Cookie("tweet_" + tweetId, hash);
            response.addCookie(cookie);

            String header = request.getHeader("Accept");

            if ("text/plain".equals(header)) {
                response.setContentType("text/plain");
                PrintWriter out = response.getWriter();
                out.print(tweetId);
            } else {
                response.sendRedirect(request.getContextPath());
            }
        }
    }


    private void printHTMLresults(HttpServletResponse response, List<Tweet> tweets,  HttpServletRequest request) throws IOException {
        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.FULL, currentLocale);
        DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.DEFAULT, currentLocale);
        response.setContentType("text/html");
        String ENCODING = "ISO-8859-1";
        response.setCharacterEncoding(ENCODING);

        PrintWriter out = response.getWriter();


        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>Wall of Tweets</title>");
        out.println("<link href=\"wot.css\" rel=\"stylesheet\" type=\"text/css\" />");
        out.println("</head>");
        out.println("<body class=\"wallbody\">");
        out.println("<h1>Wall of Tweets</h1>");
        out.println("<div class=\"walltweet\">");
        out.println("<form method=\"post\">");
        out.println("<table border=0 cellpadding=2>");
        out.println("<tr><td>Your name:</td><td><input name=\"author\" type=\"text\" size=70></td><td></td></tr>");
        out.println("<tr><td>Your tweet:</td><td><textarea name=\"tweet_text\" rows=\"2\" cols=\"70\" wrap></textarea></td>");
        out.println("<td><input type=\"submit\" name=\"action\" value=\"Tweet!\"></td></tr>");
        out.println("</table></form></div>");
        String currentDate = "None";
        for (Tweet tweet : tweets) {
            String messDate = dateFormatter.format(tweet.getCreated_at());
            if (!currentDate.equals(messDate)) {
                out.println("<br><h3>...... " + messDate + "</h3>");
                currentDate = messDate;
            }


            out.println("<div class=\"wallitem\">");
            out.println("<h4><em>" + tweet.getAuthor() + "</em> @ " + timeFormatter.format(tweet.getCreated_at()));
            if (canDeleteTweet(request, tweet.getTwid())) {
                out.println(" <form method=\"post\" style=\"display:inline;\">");
                out.println("<input type=\"hidden\" name=\"tweet_id\" value=\"" + tweet.getTwid() + "\">");
                out.println("<button type=\"submit\" style=\"color:red;\">[X]</button>");
                out.println("</form>");
            }

            out.println("</h4>");
            out.println("<p>" + tweet.getText() + "</p>");
            out.println("</div>");
        }
        out.println("</body></html>");
    }


    private void printPLAINresult(HttpServletResponse response, List<Tweet> tweets) throws IOException {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();
        out.println(tweets.size());

        for(Tweet tweet : tweets) {
            out.println(tweet.getCreated_at() + " (tweet.id = " + tweet.getTwid() + "): "
                    + tweet.getAuthor() + " wrote \"" + tweet.getText() + "\"");
        }
    }
}