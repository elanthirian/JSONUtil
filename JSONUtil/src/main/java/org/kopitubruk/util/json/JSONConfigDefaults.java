/*
 * Copyright 2015 Bill Davidson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kopitubruk.util.json;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * This class provides a singleton object which is used to change static
 * defaults used by JSONConfig and it is used as an MBean to allow JMX
 * clients with MBean support to view and modify the defaults.
 * <p>
 * Keep in mind that affects all new JSONConfig objects created in the
 * same class loader, including those created by toJSON methods which
 * don't take a JSONConfig, which could have undesirable side effects depending upon
 * your app. Use with care, if at all.  All data in this class is static.  The
 * only reason that there is an instance or instance methods is to support
 * MBean access to the defaults.  A few defaults are only available
 * programmatically.  Those are accessed statically.
 * <p>
 * It is possible to configure the default values of these flags via JNDI
 * such as is typically available in a web application by adding values to
 * your application's environment under java:/comp/env/org/kopitubruk/util/json.
 * That way you can do things like having all of the validation turned on
 * in development and testing servers and turn it off in production servers
 * for faster performance, without changing any code.
 * <p>
 * Example for Tomcat, assuming your app is named "MyApp", in
 * <code>$CATALINA_BASE/conf/Catalina/<i>host</i>/MyApp.xml</code> in order to
 * disable property name validation:
 * <pre>{@code <Context path="/MyApp">
 *   <Environment name="org/kopitubruk/util/json/validatePropertyNames" type="java.lang.Boolean" value="false" override="false" />
 * </Context>}</pre>
 * <p>
 * These are the names and their normal defaults if you don't change them.
 * See the setters for these for descriptions of what they do.
 * <h3>Validation related options.</h3>
 * <ul>
 *   <li>validatePropertyNames = true</li>
 *   <li>detectDataStructureLoops = true</li>
 *   <li>escapeBadIdentifierCodePoints = false</li>
 * </ul>
 * <h3>Safe alternate encoding options.</h3>
 * <ul>
 *   <li>encodeNumericStringsAsNumbers = false</li>
 *   <li>escapeNonAscii = false</li>
 *   <li>unEscapeWherePossible = false</li>
 *   <li>escapeSurrogates = false</li>
     <li>encodeDatesAsStrings = false</li>
 * </ul>
 * <h3>
 *   Allow generation of certain types of non-standard JSON.
 * </h3>
 * <p>
 *   Could cause problems for some things that take JSON.  Defaults are for
 *   standard JSON.  Be careful about changing these.
 * </p>
 * <ul>
 *   <li>quoteIdentifier = true</li>
 *   <li>useECMA6 = false</li>
 *   <li>allowReservedWordsInIdentifiers = false</li>
 *   <li>encodeDatesAsObjects = false</li>
 * </ul>
 * <p>
 * It is possible to set the default locale in JNDI using the name "locale" and
 * a String value that is a valid locale string that will be used to create a
 * {@link Locale}.  If you don't do this, then the
 * default locale will be whatever is provided by the JVM.
 * <p>
 * You can set the default date generation format using the name "dateGenFormat"
 * and a String value that will be passed to
 * {@link SimpleDateFormat#SimpleDateFormat(String, Locale)} using the locale
 * from {@link #getLocale()}.
 * <p>
 * You can set up to 16 default date parsing formats using the names
 * "dateParseFormat0" through "dateParseFormat15" using String values as with
 * "dateGenFormat" described above.  They will be added in numeric order of
 * the name.
 * <p>
 * It is possible to see and modify the default values of all of the boolean
 * flags at runtime if you have a JMX client with MBean support connected to
 * your server.  It will be in org.kopitubruk.util.json.JSONConfigDefaults.  You
 * can disable MBean registration by setting a boolean variable named registerMBean
 * to false in the environment as shown above for the flags.  See also the
 * {@link #clearMBean()} method for information on how to remove the MBean from
 * your server when your app is unloaded or reloaded.
 * <p>
 * There is some limited logging for access of JNDI and the MBean server.
 * Most of it is debug, so you won't see it unless you have debug logging
 * enabled for this package/class.  It might be useful to enable debug
 * logging for this class if you are having trouble with those.
 * <p>
 * You can disable JNDI lookups, MBean registration or logging by defining
 * boolean system properties for org.kopitubruk.util.json.useJNDI,
 * org.kopitubruk.util.json.registerMBean or org.kopitubruk.util.json.logging
 * respectively as false.  System properties may be set on the java command
 * line using the "-D" flag or possibly programmatically if your program has
 * permission to do it and does so before this class is loaded.
 *
 * @see JSONConfig
 * @author Bill Davidson
 */
public class JSONConfigDefaults implements JSONConfigDefaultsMBean, Serializable
{
    private static Log s_log = null;

    // Default flag values.
    private static volatile boolean validatePropertyNames;
    private static volatile boolean detectDataStructureLoops;
    private static volatile boolean escapeBadIdentifierCodePoints;

    private static volatile boolean encodeNumericStringsAsNumbers;
    private static volatile boolean escapeNonAscii;
    private static volatile boolean unEscapeWherePossible;
    private static volatile boolean escapeSurrogates;
    private static volatile boolean encodeDatesAsStrings;

    private static volatile boolean quoteIdentifier;
    private static volatile boolean useECMA6;
    private static volatile boolean allowReservedWordsInIdentifiers;
    private static volatile boolean encodeDatesAsObjects;

    // Other defaults.
    private static volatile Locale locale;
    private static volatile Map<Class<? extends Number>,NumberFormat> numberFormatMap;
    private static volatile DateFormat dateGenFormat;
    private static volatile List<DateFormat> dateParseFormats;

    // stored for deregistration on unload.
    private static ObjectName mBeanName = null;

    // the singleton, which has no instance data; only MBean methods.
    private static JSONConfigDefaults jsonConfigDefaults;

    // logging.
    private static boolean logging;

    /*
     * Initialize static data.
     */
    static {
        // the singleton.
        jsonConfigDefaults = new JSONConfigDefaults();

        // annoying that there's no static equivalent to "this".
        Class<?> thisClass = jsonConfigDefaults.getClass();

        String pkgName = thisClass.getPackage().getName();
        String registerMBeanName = "registerMBean";
        String trueStr = Boolean.TRUE.toString();

        // allow disabling JNDI, MBean and logging from the command line.
        boolean useJNDI = Boolean.parseBoolean(System.getProperty(pkgName+".useJNDI", trueStr));
        boolean registerMBean = Boolean.parseBoolean(System.getProperty(pkgName+'.'+registerMBeanName, trueStr));
        logging = Boolean.parseBoolean(System.getProperty(pkgName+".logging", trueStr));

        ResourceBundle bundle = null;
        if ( useJNDI || registerMBean ){
            bundle = JSONUtil.getBundle(getLocale());
            if ( logging ){
                s_log = LogFactory.getLog(thisClass);
            }
        }

        if ( useJNDI ){
            // Look for defaults in JNDI.
            try{
                Context ctx = JNDIUtil.getEnvContext(pkgName.replaceAll("\\.", "/"));

                // locale.
                String languageTag = JNDIUtil.getString(ctx, "locale", null);
                if ( languageTag != null ){
                    jsonConfigDefaults.setLocale(languageTag);
                    if ( logging ){
                        // possibly changed the locale.  redo the bundle.
                        bundle = JSONUtil.getBundle(getLocale());
                    }
                }

                // date generation format.
                String dgf = JNDIUtil.getString(ctx, "dateGenFormat", null);
                if ( dgf != null ){
                    jsonConfigDefaults.setDateGenFormat(dgf);
                }

                // date parse formats.
                for ( int i = 0; i < 16; i++ ){
                    String dpf = JNDIUtil.getString(ctx, "dateParseFormat" + i, null);
                    if ( dpf != null ){
                        jsonConfigDefaults.addDateParseFormat(dpf);
                    }
                }

                registerMBean = JNDIUtil.getBoolean(ctx, registerMBeanName, registerMBean);

                // validation flags.
                validatePropertyNames = JNDIUtil.getBoolean(ctx, "validatePropertyNames", validatePropertyNames);
                detectDataStructureLoops = JNDIUtil.getBoolean(ctx, "detectDataStructureLoops", detectDataStructureLoops);
                escapeBadIdentifierCodePoints = JNDIUtil.getBoolean(ctx, "escapeBadIdentifierCodePoints", escapeBadIdentifierCodePoints);

                // safe alternate encoding options.
                encodeNumericStringsAsNumbers = JNDIUtil.getBoolean(ctx, "encodeNumericStringsAsNumbers", encodeNumericStringsAsNumbers);
                escapeNonAscii = JNDIUtil.getBoolean(ctx, "escapeNonAscii", escapeNonAscii);
                unEscapeWherePossible = JNDIUtil.getBoolean(ctx, "unEscapeWherePossible", unEscapeWherePossible);
                escapeSurrogates = JNDIUtil.getBoolean(ctx, "escapeSurrogates", escapeSurrogates);
                jsonConfigDefaults.setEncodeDatesAsStrings(JNDIUtil.getBoolean(ctx, "encodeDatesAsStrings", encodeDatesAsStrings));

                // non-standard encoding options.
                quoteIdentifier = JNDIUtil.getBoolean(ctx, "quoteIdentifier", quoteIdentifier);
                useECMA6 = JNDIUtil.getBoolean(ctx, "useECMA6CodePoints", useECMA6);
                allowReservedWordsInIdentifiers = JNDIUtil.getBoolean(ctx, "allowReservedWordsInIdentifiers", allowReservedWordsInIdentifiers);
                jsonConfigDefaults.setEncodeDatesAsObjects(JNDIUtil.getBoolean(ctx, "encodeDatesAsObjects", encodeDatesAsObjects));
            }catch ( Exception e ){
                // Nothing set in JNDI.  Use code defaults.  Not a problem.
                debug(bundle.getString("badJNDIforConfig"), e);
            }
        }

        if ( registerMBean ){
            // Register an instance with MBean server if one is available.
            try{
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

                mBeanName = JNDIUtil.getObjectName(jsonConfigDefaults);
                mBeanServer.registerMBean(jsonConfigDefaults, mBeanName);
                debug(String.format(bundle.getString("registeredMbean"), mBeanName));
            }catch ( Exception e ){
                // No MBean server.  Not a problem.
                debug(bundle.getString("couldntRegisterMBean"), e);
            }
        }
    }

    /**
     * Return the JSONConfigDefaults singleton instance.
     *
     * @return the JSONConfigDefaults singleton instance.
     */
    public static JSONConfigDefaults getInstance()
    {
        return jsonConfigDefaults;
    }

    /**
     * This class should only be instantiated by the static block.  All others
     * should use {@link #getInstance()} to get that instance.
     */
    private JSONConfigDefaults()
    {
        setCodeDefaults();
    }

    /**
     * Reset all defaults to their original unmodified values.  This
     * overrides JNDI and previous MBean changes.
     */
    public void setCodeDefaults()
    {
        synchronized ( this.getClass() ){
            validatePropertyNames = true;
            detectDataStructureLoops = true;
            escapeBadIdentifierCodePoints = false;

            encodeNumericStringsAsNumbers = false;
            escapeNonAscii = false;
            unEscapeWherePossible = false;
            escapeSurrogates = false;
            encodeDatesAsStrings = false;

            quoteIdentifier = true;
            useECMA6 = false;
            allowReservedWordsInIdentifiers = false;
            encodeDatesAsObjects = false;

            locale = null;
            numberFormatMap = null;
            dateGenFormat = null;
            dateParseFormats = null;
        }
    }

    /**
     * <p>
     * When this package is used by a webapp, and you have an MBean server in your
     * environment, then you should create a ServletContextListener and call this
     * in its ServletContextListener.contextDestroyed(ServletContextEvent)
     * method to remove the MBean when the webapp is unloaded or reloaded.
     * </p>
     * <pre><code>
     * public class AppCleanUp implements ServletContextListener
     * {
     *     public void contextDestroyed( ServletContextEvent sce )
     *     {
     *         JSONConfigDefaults.clearMBean();
     *     }
     * }
     * </code></pre>
     * <p>
     * You should add it to your web.xml for your webapp like this (assuming
     * you named it org.myDomain.web.app.AppCleanUp).
     * </p>
     * <pre>{@code <listener>
     *   <listener-class>org.myDomain.web.app.AppCleanUp</listener-class>
     * </listener>}</pre>
     * <p>
     * Note that the logging from this method may not work if the logging is
     * removed/disabled before this method is called.
     */
    public static synchronized void clearMBean()
    {
        if ( mBeanName != null ){
            ResourceBundle bundle = JSONUtil.getBundle(getLocale());
            try{
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                mBeanServer.unregisterMBean(mBeanName);
                debug(String.format(bundle.getString("unregistered"), mBeanName));
            }catch ( Exception e ){
                error(String.format(bundle.getString("couldntUnregister"), mBeanName), e);
            }finally{
                // don't try again.
                mBeanName = null;
            }
        }
    }

    /**
     * Set a default locale for new JSONConfig objects to use.
     *
     * @return the default locale.
     */
    public static synchronized Locale getLocale()
    {
        return locale != null ? locale : Locale.getDefault();
    }

    /**
     * Set a default locale for new JSONConfig objects to use.
     *
     * @param loc the default locale.
     */
    public static synchronized void setLocale( Locale loc )
    {
        locale = loc;
    }

    /**
     * Set the default locale for new JSONConfig objects to use.
     *
     * @param languageTag A language tag used to create a {@link Locale}
     */
    public void setLocale( String languageTag )
    {
        if ( languageTag != null ){
            String[] loc = languageTag.split("[-_]");
            switch ( loc.length ){
                case 1: setLocale(new Locale(loc[0])); break;
                case 2: setLocale(new Locale(loc[0], loc[1])); break;
                default: setLocale(new Locale(loc[0], loc[1], loc[2])); break;
            }
        }else{
            setLocale((Locale)null);
        }
    }

    /**
     * Get the number format map.
     *
     * @return the number format map.
     */
    static Map<Class<? extends Number>,NumberFormat> getNumberFormatMap()
    {
        return numberFormatMap;
    }

    /**
     * Get the number format for the given class.
     *
     * @param numericClass A class.
     * @return A number format or null if one has not been set.
     */
    public static synchronized NumberFormat getNumberFormat( Class<? extends Number> numericClass )
    {
        return numberFormatMap != null ? numberFormatMap.get(numericClass) : null;
    }

    /**
     * Get the number format for the class of the given numeric type.
     *
     * @param num An object that implements {@link Number}.
     * @return A number format or null if one has not been set.
     */
    public static NumberFormat getNumberFormat( Number num )
    {
        return num != null ? getNumberFormat(num.getClass()) : null;
    }

    /**
     * Add a default number format for a particular type that extends Number.
     * This will be applied to all new JSONConfig objects that are created after
     * this in the same class loader.  The format is cloned for thread safety.
     *
     * @param numericClass The class.
     * @param fmt The number format.
     */
    public static void addNumberFormat( Class<? extends Number> numericClass, NumberFormat fmt )
    {
        if ( numericClass != null ){
            if ( fmt == null ){
                removeNumberFormat(numericClass);
            }else{
                Map<Class<? extends Number>,NumberFormat> numFmtMap = new HashMap<Class<? extends Number>,NumberFormat>(2);
                numFmtMap.put(numericClass, fmt);
                // handles null checking and cloning.
                addNumberFormats(numFmtMap);
            }
        }
    }

    /**
     * Add a default number format for a particular type that extends Number.
     * This will be applied to all new JSONConfig objects that are created after
     * this in the same class loader.  The format is cloned for thread safety.
     *
     * @param numericType The object.
     * @param fmt The number format.
     */
    public static void addNumberFormat( Number numericType, NumberFormat fmt )
    {
        if ( numericType != null ){
            addNumberFormat(numericType.getClass(), fmt);
        }
    }

    /**
     * Add a map of number formats to the current map of number formats.
     * The formats are cloned for thread safety.
     *
     * @param numFmtMap The input map.
     * @since 1.4
     */
    public static synchronized void addNumberFormats( Map<Class<? extends Number>,NumberFormat> numFmtMap )
    {
        numberFormatMap = mergeFormatMaps(numberFormatMap, numFmtMap);
    }

    /**
     * Merge two maps of number formats and clone the formats of the
     * source map as they are merged into the destination map.
     *
     * @param dest The destination map to be added to.
     * @param src The source map to add.
     * @return The merged map.
     */
    static Map<Class<? extends Number>,NumberFormat> mergeFormatMaps(
                                Map<Class<? extends Number>,NumberFormat> dest,
                                Map<Class<? extends Number>,NumberFormat> src )
    {
        Map<Class<? extends Number>,NumberFormat> result = dest;

        if ( src != null ){
            if ( result == null ){
                result = new HashMap<Class<? extends Number>,NumberFormat>(src);
                List<Class<? extends Number>> badKeys = null;
                // clone the formats.
                for ( Entry<Class<? extends Number>,NumberFormat> entry : result.entrySet() ){
                    if ( entry.getKey() != null && entry.getValue() != null ){
                        entry.setValue((NumberFormat)entry.getValue().clone());
                    }else{
                        // a pox on anyone who causes this to happen.
                        if ( badKeys == null ){
                            badKeys = new ArrayList<Class<? extends Number>>();
                        }
                        badKeys.add(entry.getKey());
                    }
                }
                if ( badKeys != null ){
                    // clean out the bad keys.
                    for ( Class<? extends Number> numericClass : badKeys ){
                        result.remove(numericClass);
                    }
                    if ( result.size() < 1 ){
                        result = null;
                    }else{
                        result = new HashMap<Class<? extends Number>,NumberFormat>(result);
                    }
                }
            }else{
                int size = result.size();
                for ( Entry<Class<? extends Number>,NumberFormat> entry : src.entrySet() ){
                    // only use good entries.
                    if ( entry.getKey() != null && entry.getValue() != null ){
                        result.put(entry.getKey(), (NumberFormat)entry.getValue().clone());
                    }
                }
                if ( result.size() > size ){
                    result = new HashMap<Class<? extends Number>,NumberFormat>(result);
                }
            }
        }

        return result;
    }

    /**
     * Remove the requested class from the default number formats.
     *
     * @param numericClass The class.
     */
    public static synchronized void removeNumberFormat( Class<? extends Number> numericClass )
    {
        if ( numberFormatMap != null && numericClass != null ){
            int size = numberFormatMap.size();
            numberFormatMap.remove(numericClass);
            if ( numberFormatMap.size() < 1 ){
                numberFormatMap = null;
            }else if ( numberFormatMap.size() < size ){
                numberFormatMap = new HashMap<Class<? extends Number>,NumberFormat>(numberFormatMap);
            }
        }
    }

    /**
     * Remove the requested class from the default number formats.
     *
     * @param num An object that implements {@link Number}.
     */
    public static void removeNumberFormat( Number num )
    {
        if ( num != null ){
            removeNumberFormat(num.getClass());
        }
    }

    /**
     * Clear any default number formats.  Accessible via MBean server.
     * 
     * @since 1.4
     */
    public void clearNumberFormats()
    {
        synchronized ( this.getClass() ){
            numberFormatMap = null;
        }
    }

    /**
     * Get the date string generation format.
     *
     * @return the dateFormat
     * @since 1.4
     */
    static DateFormat getDateGenFormat()
    {
        return dateGenFormat;
    }

    /**
     * Set the date string generation format.
     *
     * @param fmt the dateFormat to set
     * @since 1.4
     */
    public static synchronized void setDateGenFormat( DateFormat fmt )
    {
        dateGenFormat = fmt;
    }

    /**
     * Set the date format used for date string generation. Accessible via MBean
     * server.
     *
     * @param fmtStr passed to the constructor for
     * {@link SimpleDateFormat#SimpleDateFormat(String,Locale)} using
     * the result of {@link #getLocale()}.
     * @return the format that is created.
     * @since 1.4
     */
    public DateFormat setDateGenFormat( String fmtStr )
    {
        DateFormat fmt = null;
        if ( fmtStr != null ){
            fmt = new SimpleDateFormat(fmtStr, getLocale());
            setDateGenFormat(fmt);
        }else{
            setDateGenFormat((DateFormat)null);
        }
        return fmt;
    }

    /**
     * Clear date generation format.  Accessible via MBean server.
     *
     * @since 1.4
     */
    public void clearDateGenFormat()
    {
        synchronized ( this.getClass() ){
            dateGenFormat = null;
        }
    }

    /**
     * Get the list of date parsing formats.
     *
     * @return the list of date parsing formats.
     * @since 1.4
     */
    static List<DateFormat> getDateParseFormats()
    {
        return dateParseFormats;
    }

    /**
     * Add a date parsing format to the list of parsing formats.  When
     * parsing date strings, they will be used in the same order that
     * they were added.
     *
     * @param fmt A date parsing format.
     * @since 1.4
     */
    public static void addDateParseFormat( DateFormat fmt )
    {
        if ( fmt != null ){
            addDateParseFormats(Arrays.asList(fmt));
        }
    }

    /**
     * Add a date parsing format to the list of date parsing formats. Accessible
     * via MBean server.
     *
     * @param fmtStr Passed to
     * {@link SimpleDateFormat#SimpleDateFormat(String,Locale)} using
     * the result of {@link #getLocale()}.
     * @return The format that gets created.
     * @since 1.4
     */
    public DateFormat addDateParseFormat( String fmtStr )
    {
        DateFormat fmt = new SimpleDateFormat(fmtStr, getLocale());
        addDateParseFormat(fmt);
        return fmt;
    }

    /**
     * Add a collection of date parsing format to the list of date parsing formats.
     *
     * @param fmts A collection of date parsing formats.
     * @since 1.4
     */
    public static synchronized void addDateParseFormats( Collection<? extends DateFormat> fmts )
    {
        dateParseFormats = addDateParseFormats(dateParseFormats, fmts);
    }
    
    /**
     * Add the source collection of formats to the destination list of formats.
     *
     * @param dest The destination list.
     * @param src The source list.
     * @return The new list of formats.
     */
    static List<DateFormat> addDateParseFormats( List<DateFormat> dest, Collection<? extends DateFormat> src )
    {
        List<DateFormat> result = dest;

        if ( src != null ){
            ArrayList<DateFormat> cloneSrc = new ArrayList<DateFormat>(src.size());
            for ( DateFormat fmt : src ){
                if ( fmt != null ){
                    // clone because DateFormat's are not thread safe.
                    cloneSrc.add((DateFormat)fmt.clone());
                }
            }

            if ( cloneSrc.size() > 0 ){
                if ( result == null ){
                    // adjust size if there were nulls.
                    cloneSrc.trimToSize();
                    result = cloneSrc;
                }else{
                    List<DateFormat> tmp = new ArrayList<DateFormat>(result.size() + cloneSrc.size());
                    tmp.addAll(result);
                    tmp.addAll(cloneSrc);
                    result = tmp;
                }
            }
        }

        return result;
    }

    /**
     * Clear any date parse formats.
     */
    public void clearDateParseFormats()
    {
        synchronized ( this.getClass() ){
            dateParseFormats = null;
        }
    }

    /**
     * Get the default validate property names policy.
     * Accessible via MBean server.
     *
     * @return The default validate property names policy.
     */
    public boolean isValidatePropertyNames()
    {
        return validatePropertyNames;
    }

    /**
     * Set the default flag for validation of property names.
     * This will affect all new JSONConfig objects created after this call
     * within the same class loader.  Accessible via MBean server.
     *
     * @param dflt If true, then property names will be validated by default.
     */
    public void setValidatePropertyNames( boolean dflt )
    {
        synchronized ( this.getClass() ){
            validatePropertyNames = dflt;
        }
    }

    /**
     * Get the default detect data structure loops policy.
     * Accessible via MBean server.
     *
     * @return The default detect data structure loops policy.
     */
    public boolean isDetectDataStructureLoops()
    {
        return detectDataStructureLoops;
    }

    /**
     * Set the default flag for forcing quotes on identifiers.
     * This will affect all new JSONConfig objects created after this call
     * within the same class loader. Accessible via MBean server.
     *
     * @param dflt If true, then all identifiers will be quoted.
     */
    public void setDetectDataStructureLoops( boolean dflt )
    {
        synchronized ( this.getClass() ){
            detectDataStructureLoops = dflt;
        }
    }

    /**
     * Get the default escape bad identifier code points policy.
     * Accessible via MBean server.
     *
     * @return The default escape bad identifier code points policy.
     */
    public boolean isEscapeBadIdentifierCodePoints()
    {
        return escapeBadIdentifierCodePoints;
    }

    /**
     * If true, then any bad code points in identifiers will be escaped.
     * Default is false.  Accessible via MBean server.
     *
     * @param dflt the escapeBadIdentifierCodePoints to set
     */
    
    public void setEscapeBadIdentifierCodePoints( boolean dflt )
    {
        synchronized ( this.getClass() ){
            escapeBadIdentifierCodePoints = dflt;
        }
    }

    /**
     * Get the default encode numeric strings as numbers policy.
     * Accessible via MBean server.
     *
     * @return The default encode numeric strings as numbers policy.
     */
    
    public boolean isEncodeNumericStringsAsNumbers()
    {
        return encodeNumericStringsAsNumbers;
    }

    /**
     * Set the default flag for encoding of numeric strings as numbers.
     * Accessible via MBean server.
     *
     * @param dflt If true, then strings that look like numbers will be encoded
     * as numbers.
     */
    
    public void setEncodeNumericStringsAsNumbers( boolean dflt )
    {
        synchronized ( this.getClass() ){
            encodeNumericStringsAsNumbers = dflt;
        }
    }

    /**
     * Get the default quote non-ASCII policy.
     * Accessible via MBean server.
     *
     * @return The default quote non-ASCII policy.
     */
    
    public boolean isEscapeNonAscii()
    {
        return escapeNonAscii;
    }

    /**
     * Set the default flag for forcing escaping of non-ASCII characters.
     * This will affect all new JSONConfig objects created after this call
     * within the same class loader.  Accessible via MBean server.
     *
     * @param dflt If true, then all non-ASCII will be Unicode escaped.
     */
    
    public void setEscapeNonAscii( boolean dflt )
    {
        synchronized ( this.getClass() ){
            escapeNonAscii = dflt;
        }
    }

    /**
     * The default unEscape policy.
     * Accessible via MBean server.
     *
     * @return the unEscape policy.
     */
    
    public boolean isUnEscapeWherePossible()
    {
        return unEscapeWherePossible;
    }

    /**
     * Set default flag for undoing inline escapes in strings.
     * Accessible via MBean server.
     *
     * @param dflt If true then where possible, undo inline escapes in strings.
     */
    
    public void setUnEscapeWherePossible( boolean dflt )
    {
        synchronized ( this.getClass() ){
            unEscapeWherePossible = dflt;
        }
    }

    /**
     * The default escape surrogates policy.
     *
     * @return the s_defaultEscapeSurrogates
     */
    
    public boolean isEscapeSurrogates()
    {
        return escapeSurrogates;
    }

    /**
     * If true, then surrogates will be escaped.
     *
     * @param dflt the defaultEscapeSurrogates to set
     */
    
    public void setEscapeSurrogates( boolean dflt )
    {
        synchronized ( this.getClass() ){
            escapeSurrogates = dflt;
        }
    }

    /**
     * Get the encode dates as strings policy.
     *
     * @return the encodeDatesAsStrings policy.
     */
    public boolean isEncodeDatesAsStrings()
    {
        return encodeDatesAsStrings;
    }

    /**
     * If true, then {@link Date} objects will be encoded as
     * ISO 8601 date strings.
     *
     * @param dflt the encodeDatesAsStrings to set
     */
    public synchronized void setEncodeDatesAsStrings( boolean dflt )
    {
        synchronized ( this.getClass() ){
            encodeDatesAsStrings = dflt;
            if ( encodeDatesAsStrings ){
                encodeDatesAsObjects = false;
            }
        }
    }

    /**
     * Get the default quote identifier policy.
     *
     * @return The default quote identifier policy.
     */
    
    public boolean isQuoteIdentifier()
    {
        return quoteIdentifier;
    }

    /**
     * Set the default flag for forcing quotes on identifiers.
     * This will affect all new JSONConfig objects created after this call
     * within the same class loader. Accessible via MBean server.
     *
     * @param dflt If true, then all identifiers will be quoted.
     */
    
    public void setQuoteIdentifier( boolean dflt )
    {
        synchronized ( this.getClass() ){
            quoteIdentifier = dflt;
        }
    }

    /**
     * Get the default escape ECMAScript 6 code points policy.
     * Accessible via MBean server.
     *
     * @return The default escape ECMAScript 6 code points policy.
     */
    public boolean isUseECMA6()
    {
        return useECMA6;
    }

    /**
     * Set the default flag for using ECMAScript 6 code points to encode
     * Unicode escapes. Accessible via MBean server.
     *
     * @param dflt if true then ECMAScript 6 code points
     * will be used to encode Unicode escapes as needed.
     */
    public void setUseECMA6( boolean dflt )
    {
        synchronized ( this.getClass() ){
            useECMA6 = dflt;
        }
    }

    /**
     * Get the default for allowing reserved words in identifiers.
     *
     * @return the defaultAllowReservedWordsInIdentifiers
     */
    
    public boolean isAllowReservedWordsInIdentifiers()
    {
        return allowReservedWordsInIdentifiers;
    }

    /**
     * Set default flag for allowing reserved words in identifiers.
     *
     * @param dflt the defaultAllowReservedWordsInIdentifiers to set
     */
    
    public void setAllowReservedWordsInIdentifiers( boolean dflt )
    {
        synchronized ( this.getClass() ){
            allowReservedWordsInIdentifiers = dflt;
        }
    }

    /**
     * Get the encode dates as objects policy.
     *
     * @return the encodeDatesAsObjects policy.
     */
    public boolean isEncodeDatesAsObjects()
    {
        return encodeDatesAsObjects;
    }

    /**
     * If true, then {@link Date} objects will be encoded as
     * Javascript dates, using new Date().
     *
     * @param dflt the encodeDatesAsObjects to set
     */
    public synchronized void setEncodeDatesAsObjects( boolean dflt )
    {
        synchronized ( this.getClass() ){
            encodeDatesAsObjects = dflt;
            if ( encodeDatesAsObjects ){
                encodeDatesAsStrings = false;
            }
        }
    }

    /**
     * Shorthand wrapper around the debug logging to make other code less
     * awkward.
     *
     * @param message the message.
     * @param t the throwable.
     */
    private static void debug( String message, Throwable t )
    {
        if ( logging ){
            s_log.debug(message, t);
        }
    }

    /**
     * Shorthand wrapper around the debug logging to make other code less
     * awkward.
     *
     * @param message the message.
     */
    private static void debug( String message )
    {
        if ( logging ){
            s_log.debug(message);
        }
    }

    /**
     * Shorthand wrapper around the error logging to make other code less
     * awkward.
     *
     * @param message the message.
     * @param t the throwable.
     */
    private static void error( String message, Throwable t )
    {
        if ( logging ){
            s_log.error(message, t);
        }
    }

    private static final long serialVersionUID = 1L;
}
