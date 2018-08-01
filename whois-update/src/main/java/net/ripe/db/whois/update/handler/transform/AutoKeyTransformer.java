package net.ripe.db.whois.update.handler.transform;

import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.update.autokey.AutoKeyResolver;
import net.ripe.db.whois.update.domain.Action;
import net.ripe.db.whois.update.domain.Update;
import net.ripe.db.whois.update.domain.UpdateContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AutoKeyTransformer implements Transformer {
    private final AutoKeyResolver autoKeyResolver;

    @Autowired
    public AutoKeyTransformer(final AutoKeyResolver autoKeyResolver) {
        this.autoKeyResolver = autoKeyResolver;
    }

    @Override
    public RpslObject transform(RpslObject rpslObject, Update update, UpdateContext updateContext, final Action action) {
        return autoKeyResolver.resolveAutoKeys(rpslObject, update, updateContext, action);
    }
}
