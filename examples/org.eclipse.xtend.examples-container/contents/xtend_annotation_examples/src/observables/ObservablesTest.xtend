package observables

import java.util.Observable
import org.eclipse.xtend.core.compiler.batch.XtendCompilerTester
import org.junit.Test

class ObservableTests {

	extension XtendCompilerTester compilerTester = XtendCompilerTester::newXtendCompilerTester(typeof(Observable))

	@Test def void testObservable() {
		'''
			import observables.Observable
			
			@Observable 
			class Person {
			  String name
			}
		'''.assertCompilesTo(
		'''
			import java.beans.PropertyChangeListener;
			import java.beans.PropertyChangeSupport;
			import observables.Observable;
			
			@Observable
			@SuppressWarnings("all")
			public class Person {
			  private String name;
			  
			  public String getName() {
			    return this.name;
			  }
			  
			  public void setName(final String name) {
			    String _oldValue = this.name;
			    this.name = name;
			    _propertyChangeSupport.firePropertyChange("name", _oldValue, name);
			    
			  }
			  
			  private PropertyChangeSupport _propertyChangeSupport = new PropertyChangeSupport(this);
			  
			  public void addPropertyChangeListener(final PropertyChangeListener listener) {
			    this._propertyChangeSupport.addPropertyChangeListener(listener);
			  }
			  
			  public void removePropertyChangeListener(final PropertyChangeListener listener) {
			    this._propertyChangeSupport.removePropertyChangeListener(listener);
			  }
			}
		''')
	}
}
