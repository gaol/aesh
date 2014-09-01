/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.util.Parser;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasCompletion implements Completion {

    private static final String ALIAS = "alias";
    private static final String ALIAS_SPACE = "alias ";
    private static final String UNALIAS = "unalias";
    private static final String UNALIAS_SPACE = "unalias ";
    private static final String HELP = "--help";
    private final AliasManager manager;

    public AliasCompletion(AliasManager manager) {
        this.manager = manager;
    }

    @Override
    public void complete(CompleteOperation completeOperation) {
        completeOperation.addCompletionCandidates(manager.findAllMatchingNames(completeOperation.getBuffer().trim()));

        if(ALIAS.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(ALIAS);
        else if(UNALIAS.startsWith(completeOperation.getBuffer()))
            completeOperation.addCompletionCandidate(UNALIAS);
        else if(completeOperation.getBuffer().equals(ALIAS_SPACE) ||
                completeOperation.getBuffer().equals(UNALIAS_SPACE)) {
            completeOperation.addCompletionCandidates(manager.getAllNames());
            completeOperation.addCompletionCandidate(HELP);
            completeOperation.setOffset(completeOperation.getCursor());
        }
        else if(completeOperation.getBuffer().startsWith(ALIAS_SPACE) ||
                completeOperation.getBuffer().startsWith(UNALIAS_SPACE)) {
            String word = Parser.findWordClosestToCursor(completeOperation.getBuffer(), completeOperation.getCursor());
            completeOperation.addCompletionCandidates(manager.findAllMatchingNames(word));
            if (HELP.startsWith(word)) {
                completeOperation.addCompletionCandidate(HELP);
            }
            completeOperation.setOffset(completeOperation.getCursor()-word.length());
        }
    }

}
