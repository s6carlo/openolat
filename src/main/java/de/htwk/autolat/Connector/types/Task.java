/*
 * Warning: This is a generated file. Edit at your own risk.
 * generated by Gen.hs on Thu Feb  4 18:33:35 CET 2010.
 */

package de.htwk.autolat.Connector.types;
import java.util.List;

@SuppressWarnings("unused")
public class Task implements TaskTree
{
    private final String taskName;
    
    public Task(String taskName)
    {
        this.taskName = taskName;
    }
    
    public String getTaskName()
    {
        return taskName;
    }
    
    public String toString()
    {
        return "Task("
            + taskName + ")";
    }
    
    public boolean equals(Object other)
    {
        if (! (other instanceof Task))
            return false;
        Task oTaskTree = (Task) other;
        if (!taskName.equals(oTaskTree.getTaskName()))
            return false;
        return true;
    }
    
    public int hashCode()
    {
        return
            taskName.hashCode() * 1;
    }
    
    public Task getTask()
    {
        return this;
    }
    
    public Category getCategory()
    {
        return null;
    }
    
    public boolean isTask()
    {
        return true;
    }
    
    public boolean isCategory()
    {
        return false;
    }
    
}