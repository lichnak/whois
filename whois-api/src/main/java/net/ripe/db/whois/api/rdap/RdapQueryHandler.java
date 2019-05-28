package net.ripe.db.whois.api.rdap;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import net.ripe.db.whois.api.fulltextsearch.FullTextIndex;
import net.ripe.db.whois.api.rest.ApiResponseHandler;
import net.ripe.db.whois.common.dao.RpslObjectDao;
import net.ripe.db.whois.common.domain.ResponseObject;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.source.SourceContext;
import net.ripe.db.whois.query.acl.AccessControlListManager;
import net.ripe.db.whois.query.domain.QueryCompletionInfo;
import net.ripe.db.whois.query.domain.QueryException;
import net.ripe.db.whois.query.handler.QueryHandler;
import net.ripe.db.whois.query.planner.AbuseCFinder;
import net.ripe.db.whois.query.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.util.List;
import static net.ripe.db.whois.common.rpsl.ObjectType.*;

@Component
public class RdapQueryHandler {

    private static final int STATUS_TOO_MANY_REQUESTS = 429;
    private static final Logger LOGGER = LoggerFactory.getLogger(RdapQueryHandler.class);
    private final QueryHandler queryHandler;

    @Autowired
    public RdapQueryHandler(final QueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    public List<RpslObject> handleQuery(final Query query, final HttpServletRequest request) {

        final int contextId = System.identityHashCode(Thread.currentThread());
        final InetAddress remoteAddress = InetAddresses.forString(request.getRemoteAddr());

        final List<RpslObject> result = Lists.newArrayList();

        try {
            queryHandler.streamResults(query, remoteAddress, contextId, new ApiResponseHandler() {
                @Override
                public void handle(final ResponseObject responseObject) {
                    if (responseObject instanceof RpslObject) {
                        result.add((RpslObject) responseObject);
                    }
                }
            });

            return result;
        } catch (final QueryException e) {
            if (e.getCompletionInfo() == QueryCompletionInfo.BLOCKED) {
                throw tooManyRequests();
            } else {
                LOGGER.error(e.getMessage(), e);
                throw new IllegalStateException("query error");
            }
        }
    }

    public List<RpslObject> handleAutNumQuery(final Query query, final HttpServletRequest request) {

        final int contextId = System.identityHashCode(Thread.currentThread());
        final InetAddress remoteAddress = InetAddresses.forString(request.getRemoteAddr());

        final List<RpslObject> resultAutNum = Lists.newArrayList();
        final List<RpslObject> resultAsBlock = Lists.newArrayList();

        try {
            queryHandler.streamResults(query, remoteAddress, contextId, new ApiResponseHandler() {
                @Override
                public void handle(final ResponseObject responseObject) {
                    if (responseObject instanceof RpslObject && ((RpslObject) responseObject).getType() == AUT_NUM) {
                        resultAutNum.add((RpslObject) responseObject);

                        } else if(responseObject instanceof RpslObject && ((RpslObject) responseObject).getType() == AS_BLOCK) {
                             resultAsBlock.add((RpslObject) responseObject);
                    }
                }
            });

            return resultAutNum.isEmpty() ? resultAsBlock : resultAutNum;

        } catch (final QueryException e) {
            if (e.getCompletionInfo() == QueryCompletionInfo.BLOCKED) {
                throw tooManyRequests();
            } else {
                LOGGER.error(e.getMessage(), e);
                throw new IllegalStateException("query error");
            }
        }
    }

    public WebApplicationException tooManyRequests() {
        return new WebApplicationException(Response.status(STATUS_TOO_MANY_REQUESTS).build());
    }
}