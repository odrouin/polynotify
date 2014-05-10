import java.awt.EventQueue;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.JFrame;
import javax.swing.JButton;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JSeparator;
import javax.swing.JProgressBar;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JPasswordField;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainWindow {

	private static final String APP_VERSION = "2.1";
	private static final String NOM_FICHIER_CONFIG = "polynotify.config";

	private JFrame frmPolynotify;
	private JTextField txtCodeUsager;
	private JPasswordField txtMDP;
	private JTextField txtDateNaissance;
	private JTextField txtAdresseReception;
	private JTextField txtAdresseEnvoi;
	private JPasswordField txtMDPEnvoi;
	private JProgressBar progressBar;
	private JButton btnTest;
	private JButton btnDemarrer;
	private NotifyWorker notifyWorker;
	private JLabel lblMessage;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmPolynotify.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	private void triggerExecution() {
		if (notifyWorker == null || !notifyWorker.isExecuting()) {
			btnDemarrer.setText("Arrêter");
			btnTest.setEnabled(false);
			txtAdresseEnvoi.setEnabled(false);
			txtAdresseReception.setEnabled(false);
			txtCodeUsager.setEnabled(false);
			txtDateNaissance.setEnabled(false);
			txtMDP.setEnabled(false);
			txtMDPEnvoi.setEnabled(false);

			try {
				notifyWorker = new NotifyWorker(txtCodeUsager.getText(),
						txtMDP.getText(), txtDateNaissance.getText(),
						txtAdresseReception.getText(), txtAdresseEnvoi.getText(),
						txtMDPEnvoi.getText(), this);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			notifyWorker.execute();
		} else {
			btnDemarrer.setText("Démarrer...");
			btnTest.setEnabled(true);
			txtAdresseEnvoi.setEnabled(true);
			txtAdresseReception.setEnabled(true);
			txtCodeUsager.setEnabled(true);
			txtDateNaissance.setEnabled(true);
			txtMDP.setEnabled(true);
			txtMDPEnvoi.setEnabled(true);
			updateProgress(0);

			notifyWorker.stop();
		}
	}

	private void sendTestEmailTo(String to) {
		String text;

		text = "Test";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(txtAdresseEnvoi
								.getText(), txtMDPEnvoi.getText());
					}
				});

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(txtAdresseEnvoi.getText()));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));
			message.setSubject("[PolyNotify] Message de test");
			message.setText(text);

			Transport.send(message);

			writeMessage("Message de test envoyé avec succès");

		} catch (MessagingException e) {
			writeErrorMessage("Échec de l'envoi du message de test!");
		}
	}

	public void writeMessage(String message) {
		lblMessage.setForeground(Color.BLACK);
		lblMessage.setText(message);
	}

	public void writeErrorMessage(String message) {
		lblMessage.setForeground(Color.RED);
		lblMessage.setText(message);
	}

	public void updateProgress(int progress) {
		progressBar.setValue(progress);
	}

	private void LoadFromConfigFile() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					NOM_FICHIER_CONFIG));

			txtCodeUsager.setText(reader.readLine());
			txtDateNaissance.setText(reader.readLine());
			txtAdresseReception.setText(reader.readLine());
			txtAdresseEnvoi.setText(reader.readLine());

			reader.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void SaveToConfigFile() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(
					NOM_FICHIER_CONFIG));

			out.write(txtCodeUsager.getText() + "\n");
			out.write(txtDateNaissance.getText() + "\n");
			out.write(txtAdresseReception.getText() + "\n");
			out.write(txtAdresseEnvoi.getText());

			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmPolynotify = new JFrame();
		frmPolynotify.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent arg0) {
				LoadFromConfigFile();
			}

			@Override
			public void windowClosing(WindowEvent e) {
				SaveToConfigFile();
			}
		});
		frmPolynotify.setTitle("PolyNotify - " + "v" + APP_VERSION);
		frmPolynotify.setBounds(100, 100, 363, 357);
		frmPolynotify.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		txtCodeUsager = new JTextField();
		txtCodeUsager.setBounds(166, 8, 171, 20);
		txtCodeUsager.setColumns(10);

		JLabel lblCode = new JLabel("Code d'acc\u00E8s: ");
		lblCode.setBounds(10, 11, 146, 14);

		JLabel lblMotDePasse = new JLabel("Mot de passe: ");
		lblMotDePasse.setBounds(10, 39, 146, 14);

		txtMDP = new JPasswordField();
		txtMDP.setBounds(166, 36, 171, 20);
		txtMDP.setColumns(10);

		JLabel lblDateNaissance = new JLabel("Date de naissance: ");
		lblDateNaissance.setBounds(10, 67, 146, 14);

		txtDateNaissance = new JTextField();
		txtDateNaissance.setBounds(166, 64, 171, 20);
		txtDateNaissance.setColumns(10);

		JSeparator separator = new JSeparator();
		separator.setBounds(47, 95, 251, 5);

		JLabel lblAdresseReception = new JLabel("Adresse de r\u00E9ception: ");
		lblAdresseReception.setBounds(10, 110, 146, 14);

		txtAdresseReception = new JTextField();
		txtAdresseReception.setBounds(166, 107, 171, 20);
		txtAdresseReception.setColumns(10);

		JLabel lblAdresseEnvoi = new JLabel("Adresse d'envoi (GMail):");
		lblAdresseEnvoi.setBounds(10, 138, 146, 14);

		txtAdresseEnvoi = new JTextField();
		txtAdresseEnvoi.setBounds(166, 135, 171, 20);
		txtAdresseEnvoi.setColumns(10);

		JLabel lblMDPEnvoi = new JLabel("Mot de passe d'envoi:");
		lblMDPEnvoi.setBounds(10, 166, 146, 14);

		txtMDPEnvoi = new JPasswordField();
		txtMDPEnvoi.setBounds(166, 163, 171, 20);
		txtMDPEnvoi.setColumns(10);

		btnTest = new JButton("Test...");
		btnTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				sendTestEmailTo(txtAdresseReception.getText());
			}
		});
		btnTest.setBounds(10, 246, 100, 26);

		btnDemarrer = new JButton("D\u00E9marrer...");
		btnDemarrer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				triggerExecution();
			}
		});
		btnDemarrer.setBounds(237, 246, 100, 26);

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 280, 327, 28);
		progressBar.setMaximum(NotifyWorker.SLEEP_TIME);

		lblMessage = new JLabel("");
		lblMessage.setBounds(10, 191, 327, 44);
		lblMessage.setForeground(Color.RED);
		lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		frmPolynotify.getContentPane().setLayout(null);
		frmPolynotify.getContentPane().add(txtCodeUsager);
		frmPolynotify.getContentPane().add(lblCode);
		frmPolynotify.getContentPane().add(lblMotDePasse);
		frmPolynotify.getContentPane().add(txtMDP);
		frmPolynotify.getContentPane().add(lblDateNaissance);
		frmPolynotify.getContentPane().add(txtDateNaissance);
		frmPolynotify.getContentPane().add(separator);
		frmPolynotify.getContentPane().add(lblAdresseReception);
		frmPolynotify.getContentPane().add(txtAdresseReception);
		frmPolynotify.getContentPane().add(lblAdresseEnvoi);
		frmPolynotify.getContentPane().add(txtAdresseEnvoi);
		frmPolynotify.getContentPane().add(lblMDPEnvoi);
		frmPolynotify.getContentPane().add(txtMDPEnvoi);
		frmPolynotify.getContentPane().add(btnTest);
		frmPolynotify.getContentPane().add(btnDemarrer);
		frmPolynotify.getContentPane().add(progressBar);
		frmPolynotify.getContentPane().add(lblMessage);
	}
}
