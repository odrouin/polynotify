import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.SwingWorker;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class NotifyWorker extends SwingWorker<Void, Void> {

	public class NoConnectionException extends Exception {
		private static final long serialVersionUID = -4262061319398937230L;
	}

	public static final int SLEEP_TIME = 5 * 60 * 1000; // in milliseconds

	private String code;
	private String pw;
	private String naissance;
	private String emailTo;
	private String emailFrom;
	private String pwFrom;
	private MainWindow win;

	private boolean exec;

	public NotifyWorker(String code, String pw, String naissance,
			String emailTo, String emailFrom, String pwFrom, MainWindow win)
			throws GeneralSecurityException, IOException {
		this.code = code;
		this.pw = pw;
		this.naissance = naissance;
		this.emailTo = emailTo;
		this.emailFrom = emailFrom;
		this.pwFrom = pwFrom;
		this.win = win;

		exec = true;
	}

	public void stop() {
		exec = false;
	}

	public boolean isExecuting() {
		return exec;
	}

	@Override
	protected Void doInBackground() {
		String lastNotesContent = "";

		while (exec) {

			try {
				String urlParameters = null;
				try {
					urlParameters = "code=" + URLEncoder.encode(code, "UTF-8")
							+ "&nip=" + URLEncoder.encode(pw, "UTF-8")
							+ "&naissance="
							+ URLEncoder.encode(naissance, "UTF-8");
					String webPage = "";
					try {
						webPage = executePost(
								"https://www4.polymtl.ca/servlet/ValidationServlet",
								urlParameters);
					} catch (Exception e) {
					}

					int index, indexEnd;

					index = webPage
							.indexOf("<html><head><title>S&#233;lection d'une fonction</title>");

					if (index == -1) {
						throw new NoConnectionException();
					}

					String varName = "";
					String varValue = null;
					index = webPage.indexOf("<p>&nbsp;</p></div>");

					urlParameters = "";

					index = webPage.indexOf("name", index);
					index += 6;
					indexEnd = webPage.indexOf("\"", index);
					varName = webPage.substring(index, indexEnd);

					index = webPage.indexOf("value", index);
					index += 7;
					indexEnd = webPage.indexOf("\"", index);
					varValue = webPage.substring(index, indexEnd);

					urlParameters += varName + "="
							+ URLEncoder.encode(varValue, "UTF-8");

					while (!varName.equals("conflits09")) {
						index = webPage.indexOf("name", index);
						index += 6;
						indexEnd = webPage.indexOf("\"", index);
						varName = webPage.substring(index, indexEnd);

						index = webPage.indexOf("value", index);
						index += 7;
						indexEnd = webPage.indexOf("\"", index);
						varValue = webPage.substring(index, indexEnd);

						urlParameters += "&" + varName + "="
								+ URLEncoder.encode(varValue, "UTF-8");
					}

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				String webPage = "";
				try {
					webPage = executePost(
							"https://www4.polymtl.ca/servlet/PresentationResultatsTrimServlet",
							urlParameters);
				} catch (Exception e) {
				}

				int index, indexEnd;
				index = webPage.indexOf("<br>     N O T E S &#160;");
				indexEnd = webPage.indexOf("&nbsp;L&Eacute;GENDE");

				if (index == -1) {
					throw new NoConnectionException();
				}

				if (lastNotesContent.equals(webPage.substring(index, indexEnd))
						|| lastNotesContent.equals("")) {
					win.writeMessage("Derni�re v�rification: " + actualTime());
				} else {
					sendEmailTo(emailTo, emailFrom, pwFrom);
				}

				lastNotesContent = webPage.substring(index, indexEnd);
			} catch (NoConnectionException exception) {
				win.writeErrorMessage("�chec de la connexion: " + actualTime());
			}

			int elapsedTime = 0;
			while (elapsedTime < SLEEP_TIME && exec) {
				win.updateProgress(elapsedTime);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				elapsedTime += 500;
			}

		}

		return null;
	}

	private String executePost(String targetURL, String urlParameters) {

		URL url;
		HttpURLConnection connection = null;
		try {
			// Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

		} catch (Exception e) {

			return "";

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private void sendEmailTo(String to, final String from, final String pw) {
		String text;

		text = "https://www4.polymtl.ca/poly/poly.html";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(from, pw);
					}
				});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));
			message.setSubject("[PolyNotify] Avis de modification de note");
			message.setText(text);

			Transport.send(message);

			win.writeMessage("Courriel envoy� avec succ�s");

		} catch (MessagingException e) {
			win.writeErrorMessage("�chec de l'envoi du message!");
		}
	}

	public String actualTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(cal.getTime());
	}
}
