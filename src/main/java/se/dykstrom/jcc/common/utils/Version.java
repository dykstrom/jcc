/*
 * Copyright (C) 2016 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.common.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Provides the current version of JCC.
 *
 * @author Johan Dykstrom
 */
public final class Version {

    private String version = "<no version>";

    /**
     * The singleton instance of this class.
     */
    private static final class Holder {
        public static final Version INSTANCE = new Version();
    }

    /**
     * Returns the singleton {@code Version} instance.
     *
     * @return The singleton {@code Version} instance.
     */
    @SuppressWarnings("SameReturnValue")
    public static Version instance() {
        return Holder.INSTANCE;
    }

    private Version() {
        try {
            URL url = Version.class.getResource("/version.properties");
            if (url != null) {
                Properties properties = new Properties();
                properties.load(url.openStream());
                version = properties.getProperty("jcc.version");
            }
        } catch (IOException ignore) { 
            // Ignore
        }
    }

    @Override
    public String toString() {
        return version;
    }
}
