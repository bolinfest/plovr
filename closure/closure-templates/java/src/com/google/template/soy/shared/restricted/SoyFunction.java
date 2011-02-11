/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.shared.restricted;



/**
 * Superinterface for a Soy function.
 *
 * <p> Important: Implementing this interface by itself does nothing. Your function implementation
 * class needs to implement some of all of this interface's subinterfaces.
 *
 * @author Kai Huang
 */
public interface SoyFunction {


  /**
   * Gets the name of the Soy function.
   * @return The name of the Soy function.
   */
  public String getName();


  /**
   * @param numArgs a number of arguments to the function
   * @return whether numArgs is an appropriate number of arguments to pass
   */
  public boolean isValidArgsSize(int numArgs);
}
