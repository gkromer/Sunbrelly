package com.example.myapplication;
// you need the following imports in all Java Bridge apps
import com.google.appinventor.components.runtime.HandlesEventDispatching;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.Component;

// this particular sample has a single button, so import class for that
import com.google.appinventor.components.runtime.Button;
import com.google.appinventor.components.runtime.NxtLightSensor;
import com.google.appinventor.components.runtime.WebViewer;


public class BridgeClass extends Form implements HandlesEventDispatching {

    private Button redButton;
    private Button motorOnButton;
    private Button motorOffButton;
    private WebViewer webViewer;
    private NxtLightSensor lightSensor;

    // $define is where you'll put code for initialization, so any properties you need set or code
    // that you'd put in Screen.Initialize for App Inventor
    protected void $define()
    {
        redButton = new Button(this);  // parameter defines the parent, in this case the screen.
        motorOnButton = new Button(this);  // parameter defines the parent, in this case the screen.
        motorOffButton = new Button(this);  // parameter defines the parent, in this case the screen.
        webViewer = new WebViewer(this);  // parameter defines the parent, in this case the screen.
        lightSensor = new NxtLightSensor(this);
        // if you had an arrangement, you'd refer to it
        redButton.Text( "RED" );
        motorOnButton.Text( "Motor On" );
        motorOffButton.Text( "Motor Off" );
        redButton.BackgroundColor( 0xFF00FF00 );  // 0x means hex, FF is opaque transparency, 00FF00 is rgb, so green

        EventDispatcher.registerEventForDelegation( this, "RedButton", "Click" );
        EventDispatcher.registerEventForDelegation( this, "MotorOnButton", "Click" );
        EventDispatcher.registerEventForDelegation( this, "MotorOffButton", "Click" );
        EventDispatcher.registerEventForDelegation( this, "LightSensor", "LightChanged" );

    }
    public boolean dispatchEvent(Component component, String componentName, String eventName, Object[] params )
    {
        if( component.equals(redButton) && eventName.equals("Click") )
        {
            RedButtonClick();
            return true;
        }
        if( component.equals(motorOnButton) && eventName.equals("Click") )
        {
            MotorOnButton();
            webViewer.Height(1200);
            webViewer.Width(800);
            webViewer.getView();
            webViewer.GoToUrl("http://facebook.com");
            return true;
        }
        if( component.equals(lightSensor) && eventName.equals("LightChanged") )
        {
            RedButtonClick();
            return true;
        }
        return false;
    }
    public void RedButtonClick()
    {
        redButton.BackgroundColor(0xFFFF0000);
    }
    public void MotorOnButton()
    {
        motorOnButton.BackgroundColor(0xFFFF0000);
    }
    public void MotorOffButton()
    {
        motorOffButton.BackgroundColor(0xFFFF0000);
    }


}

