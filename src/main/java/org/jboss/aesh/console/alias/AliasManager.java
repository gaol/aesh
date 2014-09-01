/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages Aliases
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManager {

    private final List<Alias> aliases;
    private final Pattern aliasPattern = Pattern.compile("^(alias)\\s+(\\w+)\\s*=\\s*(.*)$");
    private final Pattern listAliasPattern = Pattern.compile("^(alias)((\\s+\\w+)+)$");
    private final Pattern aliasHelpPattern = Pattern.compile("^(" + ALIAS + ")\\s+\\-\\-help$");
    private final Pattern unaliasHelpPattern = Pattern.compile("^(" + UNALIAS + ")\\s+\\-\\-help$");
    private static final String ALIAS = "alias";
    private static final String ALIAS_SPACE = "alias ";
    private static final String UNALIAS = "unalias";

    public AliasManager(File aliasFile) throws IOException {
        aliases = new ArrayList<Alias>();
        if(aliasFile != null && aliasFile.isFile())
            readAliasesFromFile(aliasFile);
    }

    private void readAliasesFromFile(File aliasFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(aliasFile));
        try {
            String line;
            while((line = br.readLine()) != null) {
                if(line.startsWith(ALIAS)) {
                    try {
                        parseAlias(line);
                    }
                    catch (Exception ignored) {
                    }
                }
            }
        }
        finally {
            br.close();
        }
    }

    public void persist() {
        //TODO: implementation
    }

    public void addAlias(String name, String value) {
        Alias alias = new Alias(name, value);
        if(aliases.contains(alias)) {
            aliases.remove(alias);
        }
        aliases.add(alias);
    }

    @SuppressWarnings("unchecked")
    public String printAllAliases() {
        StringBuilder sb = new StringBuilder();
        Collections.sort(aliases); // not very efficient, but it'll do for now...
        for(Alias a : aliases)
            sb.append(ALIAS_SPACE).append(a.toString()).append(Config.getLineSeparator());

        return sb.toString();
    }

    public Alias getAlias(String name) {
        int index = aliases.indexOf(new Alias(name, null));
        if(index > -1)
            return aliases.get(index);
        else
            return null;
    }

    public List<String> findAllMatchingNames(String name) {
        List<String> names = new ArrayList<String>();
        for(Alias a : aliases)
            if(a.getName().startsWith(name))
                names.add(a.getName());

        return names;
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<String>();
        for(Alias a : aliases)
            names.add(a.getName());

        return names;
    }

    public String removeAlias(String buffer) {
        if(buffer.trim().equals(UNALIAS))
            return unaliasUsage();
        if (unaliasHelpPattern.matcher(buffer).matches())
            return unaliasUsage();

        buffer = buffer.substring(UNALIAS.length()).trim();
        for(String s : buffer.split(" ")) {
            if(s != null) {
                Alias a = getAlias(s.trim());
                if(a != null)
                    aliases.remove(a);
                else
                    return Settings.getInstance().getName()+": unalias: "+s+": not found"
                            +Config.getLineSeparator();
            }
        }
        return null;
    }

    public String parseAlias(String buffer) {
        if(buffer.trim().equals(ALIAS))
            return printAllAliases();
        if (aliasHelpPattern.matcher(buffer).matches())
            return aliasUsage();
        Matcher aliasMatcher = aliasPattern.matcher(buffer);
        if(aliasMatcher.matches()) {
            String name = aliasMatcher.group(2);
            String value = aliasMatcher.group(3);
            if(value.startsWith("'")) {
                if(value.endsWith("'"))
                    value = value.substring(1,value.length()-1);
                else
                    return aliasUsage();
            }
            else if(value.startsWith("\"")) {
                if(value.endsWith("\""))
                    value = value.substring(1,value.length()-1);
                else
                    return aliasUsage();
            }
            if(name.contains(" "))
                return aliasUsage();

            addAlias(name, value);
            return null;
        }

        Matcher listMatcher = listAliasPattern.matcher(buffer);
        if(listMatcher.matches()) {
            StringBuilder sb = new StringBuilder();
                for(String s : listMatcher.group(2).trim().split(" ")) {
                if(s != null) {
                    Alias a = getAlias(s.trim());
                    if(a != null)
                        sb.append(ALIAS_SPACE).append(a.getName()).append("='")
                                .append(a.getValue()).append("'").append(Config.getLineSeparator());
                    else
                        sb.append(Settings.getInstance().getName()).append(": alias: ").append(s)
                                .append(" : not found").append(Config.getLineSeparator());
                }
            }
            return sb.toString();
        }
        return null;
    }

    private String aliasUsage() {
        return "alias: usage: alias [name[=value] ... ]"+Config.getLineSeparator();
    }

    private String unaliasUsage() {
        return "unalias: usage: unalias name [name ...]"+Config.getLineSeparator();
    }

}
