package petitus.petcareplus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageSourceService {
    private final MessageSource messageSource;

    public String get(final String code, final Object[] params, final Locale locale) {
        try {
            return messageSource.getMessage(code, params, locale);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }

    public String get(final String code, final Object[] params) {
        return get(code, params, LocaleContextHolder.getLocale());
    }

    public String get(final String code, final Locale locale) {
        return get(code, new Object[0], locale);
    }

    public String get(final String code) {
        return get(code, new Object[0], LocaleContextHolder.getLocale());
    }
}