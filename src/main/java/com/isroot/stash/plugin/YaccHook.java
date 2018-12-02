package com.isroot.stash.plugin;

import com.atlassian.bitbucket.event.branch.BranchCreationHookRequest;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHook;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.RepositoryHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryHookTrigger;
import com.atlassian.bitbucket.hook.repository.RepositoryPushHookRequest;
import com.atlassian.bitbucket.hook.repository.StandardRepositoryHookTrigger;
import com.atlassian.bitbucket.repository.Branch;
import com.atlassian.bitbucket.setting.Settings;
import com.isroot.stash.plugin.checks.BranchNameCheck;
import com.isroot.stash.plugin.errors.YaccError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Sean Ford
 * @since 2013-05-11
 */
public class YaccHook implements PreRepositoryHook {
    private static final Logger log = LoggerFactory.getLogger(YaccHook.class);

    private final YaccService yaccService;

    public YaccHook(YaccService yaccService) {
        this.yaccService = yaccService;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(
            @Nonnull PreRepositoryHookContext context,
            @Nonnull RepositoryHookRequest repositoryHookRequest) {
        log.debug("preUpdate, class={}, trigger={}", repositoryHookRequest.getClass()
                .getSimpleName(), repositoryHookRequest.getTrigger());

        log.debug("yacc settings: {}", context.getSettings().asMap());

        final RepositoryHookTrigger trigger = repositoryHookRequest.getTrigger();

        if (trigger == StandardRepositoryHookTrigger.REPO_PUSH) {
            return handleRepositoryPush(context, (RepositoryPushHookRequest) repositoryHookRequest);
        } else if (trigger == StandardRepositoryHookTrigger.BRANCH_CREATE) {
            return handleBranchCreation(context.getSettings(),
                    (BranchCreationHookRequest) repositoryHookRequest);
        }

        return RepositoryHookResult.accepted();
    }

    private RepositoryHookResult handleRepositoryPush(
            @Nonnull PreRepositoryHookContext context,
            @Nonnull RepositoryPushHookRequest repositoryPushHookRequest) {
        final Settings settings = context.getSettings();

        return yaccService.check(context, repositoryPushHookRequest, settings);
    }

    static RepositoryHookResult handleBranchCreation(@Nonnull Settings settings,
            @Nonnull BranchCreationHookRequest branchCreationHookRequest) {
        final Branch branch = branchCreationHookRequest.getBranch();

        log.debug("branch creation hook: id={} displayId={}",
                branch.getId(), branch.getDisplayId());

        List<YaccError> errors = new BranchNameCheck(settings, branch.getId()).check();

        if (!errors.isEmpty()) {
            return RepositoryHookResult.rejected(
                    "Branch name does not comply with repository requirements.",
                    errors.get(0).getMessage());
        }

        return RepositoryHookResult.accepted();
    }
}
