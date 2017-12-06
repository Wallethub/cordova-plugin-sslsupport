#import <Foundation/Foundation.h>
#import <AFNetworking/AFNetworking.h>

@interface TextResponseSerializer : AFHTTPResponseSerializer

+ (instancetype)serializer;

@end