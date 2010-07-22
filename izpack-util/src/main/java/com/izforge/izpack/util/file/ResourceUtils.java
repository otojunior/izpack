/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.izforge.izpack.util.file;

import java.io.File;
import java.util.Vector;

import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.file.types.Resource;
import com.izforge.izpack.util.file.types.ResourceFactory;
import com.izforge.izpack.util.file.types.selectors.SelectorUtils;

/**
 * this class provides utility methods to process resources
 */
public class ResourceUtils
{

    /**
     * tells which source files should be reprocessed based on the
     * last modification date of target files
     *
     * @param logTo   where to send (more or less) interesting output
     * @param source  array of resources bearing relative path and last
     *                modification date
     * @param mapper  filename mapper indicating how to find the target
     *                files
     * @param targets object able to map as a resource a relative path
     *                at <b>destination</b>
     * @return array containing the source files which need to be
     *         copied or processed, because the targets are out of date or do
     *         not exist
     */
    public static Resource[] selectOutOfDateSources(Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets)
            throws Exception
    {
        return selectOutOfDateSources(source, mapper, targets,
                FileUtils.getFileUtils()
                        .getFileTimestampGranularity());
    }

    /**
     * tells which source files should be reprocessed based on the
     * last modification date of target files
     *
     * @param logTo       where to send (more or less) interesting output
     * @param source      array of resources bearing relative path and last
     *                    modification date
     * @param mapper      filename mapper indicating how to find the target
     *                    files
     * @param targets     object able to map as a resource a relative path
     *                    at <b>destination</b>
     * @param granularity The number of milliseconds leeway to give
     *                    before deciding a target is out of date.
     * @return array containing the source files which need to be
     *         copied or processed, because the targets are out of date or do
     *         not exist
     */
    public static Resource[] selectOutOfDateSources(Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets,
                                                    long granularity)
            throws Exception
    {
        long now = (new java.util.Date()).getTime() + granularity;

        Vector<Resource> vresult = new Vector<Resource>();
        for (int counter = 0; counter < source.length; counter++)
        {
            if (source[counter].getLastModified() > now)
            {
                Debug.log("Warning: " + source[counter].getName()
                        + " modified in the future.");
            }

            String[] targetnames =
                    mapper.mapFileName(source[counter].getName()
                            .replace('/', File.separatorChar));
            if (targetnames != null)
            {
                boolean added = false;
                StringBuffer targetList = new StringBuffer();
                for (int ctarget = 0; !added && ctarget < targetnames.length;
                     ctarget++)
                {
                    Resource atarget =
                            targets.getResource(targetnames[ctarget]
                                    .replace(File.separatorChar, '/'));
                    // if the target does not exist, or exists and
                    // is older than the source, then we want to
                    // add the resource to what needs to be copied
                    if (!atarget.isExists())
                    {
                        Debug.log(source[counter].getName() + " added as "
                                + atarget.getName()
                                + " doesn\'t exist.");
                        vresult.addElement(source[counter]);
                        added = true;
                    }
                    else if (!atarget.isDirectory()
                            && SelectorUtils.isOutOfDate(source[counter],
                            atarget,
                            (int) granularity))
                    {
                        Debug.log(source[counter].getName() + " added as "
                                + atarget.getName()
                                + " is outdated.");
                        vresult.addElement(source[counter]);
                        added = true;
                    }
                    else
                    {
                        if (targetList.length() > 0)
                        {
                            targetList.append(", ");
                        }
                        targetList.append(atarget.getName());
                    }
                }

                if (!added)
                {
                    Debug.log(source[counter].getName()
                            + " omitted as " + targetList.toString()
                            + (targetnames.length == 1 ? " is" : " are ")
                            + " up to date.");
                }
            }
            else
            {
                Debug.log(source[counter].getName()
                        + " skipped - don\'t know how to handle it");
            }
        }
        Resource[] result = new Resource[vresult.size()];
        vresult.copyInto(result);
        return result;
    }
}
