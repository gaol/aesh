/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.edit;

import org.jboss.aesh.edit.actions.Action;
import org.jboss.aesh.edit.actions.Operation;
import org.jboss.aesh.terminal.Key;

/**
 * Trying to follow the Emacs mode GNU Readline impl found here:
 * http://cnswww.cns.cwru.edu/php/chet/readline/readline.html
 *
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public class EmacsEditMode extends AbstractEditMode {

    private Action mode = Action.EDIT;

    private final KeyOperationManager operationManager;

    public EmacsEditMode(KeyOperationManager operations) {
        this.operationManager = operations;
    }

    @Override
    public Operation parseInput(Key in, String buffer) {

        if(isAskingForCompletions()) {
            if(in == Key.y) {
                setAskForCompletions(false);
                return Operation.COMPLETE;
            }
            else if(in == Key.n) {
                setAskForCompletions(false);
                return Operation.COMPLETE_ABORT;
            }
            else {
                return Operation.NO_ACTION;
            }
        }

        KeyOperation currentOperation = operationManager.findOperation(in);
        if(currentOperation != null)
            return findOperation(currentOperation, buffer);
        else if (mode == Action.SEARCH) {
            if(in == Key.ESC) {
                mode = Action.EDIT;
                return Operation.SEARCH_EXIT;
            }
            else
                return Operation.SEARCH_INPUT;
        }
        else if(in == Key.ESC)
            return Operation.NO_ACTION;
        else
            return Operation.EDIT;
    }

    private Operation findOperation(KeyOperation currentOperation, String buffer) {
        //just make sure that we always clear eof when we get a new operation
        if(currentOperation.getOperation() != Operation.EOF)
            resetEOF();
        //search mode need special handling
        if(mode == Action.SEARCH) {
                if(currentOperation.getOperation() == Operation.NEW_LINE) {
                    mode = Action.EDIT;
                    return Operation.SEARCH_END;
                }
                else if(currentOperation.getOperation() == Operation.SEARCH_PREV) {
                    return Operation.SEARCH_PREV_WORD;
                }
                else if(currentOperation.getOperation() == Operation.SEARCH_NEXT_WORD) {
                    return Operation.SEARCH_NEXT_WORD;
                }
                else if(currentOperation.getOperation() == Operation.DELETE_PREV_CHAR) {
                    return Operation.SEARCH_DELETE;
                }
                //if we got more than one we know that it started with esc
                // search input
                else {
                    return Operation.SEARCH_INPUT;
                }
        } // end search mode
        else {
            // process if we have any hits...
            Operation operation = currentOperation.getOperation();
            if(operation == Operation.SEARCH_PREV ||
                    operation == Operation.SEARCH_NEXT_WORD)
                mode = Action.SEARCH;

            //if ctrl-d is pressed on an empty line we update the eofCounter
            // if eofCounter > ignoreEof we send EXIT operation, else NO_ACTION
            //if buffer is not empty, we send a NEW_LINE
            if(currentOperation.getOperation() == Operation.EOF) {
                if(buffer.isEmpty()) {
                    checkEof();
                    eofCounter++;
                    if(eofCounter > ignoreEof)
                        return currentOperation.getOperation();
                    else
                        return Operation.IGNOREEOF;
                }
                else {
                    resetEOF();
                    return Operation.DELETE_NEXT_CHAR;
                }
            }

            return operation;
        }
    }

    @Override
    public Action getCurrentAction() {
        return mode;
    }

    @Override
    public Mode getMode() {
        return Mode.EMACS;
    }
}
