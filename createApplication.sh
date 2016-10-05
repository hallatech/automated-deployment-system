#!/bin/bash
# Place holders for respected ear files and examaple configurations
            
case "$1"   in  
            LiveStoreFront)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/LiveStoreFront/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/LiveStoreFront/server/slot/deploy/LiveStoreFront.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/LiveStoreFront/localconfig
            ;;
            StagingStoreFront)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/StagingStoreFront/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/StagingStoreFront/server/slot/deploy/StagingStoreFront.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/StagingStoreFront/localconfig
            ;;
            ContentAdmin)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/ContentAdmin/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/ContentAdmin/server/slot/deploy/ContentAdmin.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/ContentAdmin/localconfig
            ;;
            Listings)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/Listings/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/Listings/server/slot/deploy/Listings.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/Listings/localconfig
            ;;
            Affiliates)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/Affiliates/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/Affiliates/server/slot/deploy/Affiliates.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/Affiliates/localconfig
            ;;
            CSC)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/CSC/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/CSC/server/slot/deploy/CSC.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/CSC/localconfig
            ;;
            all)
                /bin/mkdir -p ~/workspace/Input/BuildInputs/LiveStoreFront/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/LiveStoreFront/server/slot/deploy/LiveStoreFront.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/LiveStoreFront/localconfig
                /bin/mkdir -p ~/workspace/Input/BuildInputs/StagingStoreFront/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/StagingStoreFront/server/slot/deploy/StagingStoreFront.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/StagingStoreFront/localconfig
                /bin/mkdir -p ~/workspace/Input/BuildInputs/ContentAdmin/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/ContentAdmin/server/slot/deploy/ContentAdmin.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/ContentAdmin/localconfig
                /bin/mkdir -p ~/workspace/Input/BuildInputs/Listings/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/Listings/server/slot/deploy/Listings.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/Listings/localconfig
                /bin/mkdir -p ~/workspace/Input/BuildInputs/Affiliates/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/Affiliates/server/slot/deploy/Affiliates.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/Affiliates/localconfig
                /bin/mkdir -p ~/workspace/Input/BuildInputs/CSC/server/slot/deploy/
                /bin/touch ~/workspace/Input/BuildInputs/CSC/server/slot/deploy/CSC.ear
                /bin/mkdir -p ~/workspace/Input/ExampleConfig/CSC/localconfig
            ;;  

        *)
            echo $"Usage: $0 {LiveStoreFront|StagingStoreFront|ContentAdmin|Listings|Affiliates|CSC|all}"
            exit 1

esac
