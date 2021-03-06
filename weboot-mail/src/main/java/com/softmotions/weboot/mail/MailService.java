package com.softmotions.weboot.mail;

import java.util.List;

import jodd.mail.SendMailSessionProvider;

/**
 * Basic mail-senging service.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface MailService extends SendMailSessionProvider {

    /**
     * Creates new mail instance.
     * Mail can be send by calling {@link Mail#send()}
     * Mail from address initially set from app env configuration.
     */
    Mail newMail();

    /**
     * Return history of sent emails.
     * @return
     */
    List<Mail> getHistory();
}
