#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

@interface CordovaPluginSslSupport  : CDVPlugin

- (void)enableSSLPinning:(CDVInvokedUrlCommand*)command;
- (void)acceptAllCerts:(CDVInvokedUrlCommand*)command;
- (void)validateDomainName:(CDVInvokedUrlCommand*)command;
- (void)setHeader:(CDVInvokedUrlCommand*)command;
- (void)post:(CDVInvokedUrlCommand*)command;
- (void)get:(CDVInvokedUrlCommand*)command;
- (void)cancelRequest:(CDVInvokedUrlCommand*)command;

@end
