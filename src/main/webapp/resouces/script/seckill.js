//存放主要交互逻辑js代码
//javascript 模块化
var seckill={
    //封装秒杀相关ajax的url
    URL : {
        now : function(){
            return "/will/seckill/time/now";
        },
        exposer : function(seckillId){
            return "/will/seckill/"+seckillId+"/exposer";
        },
        execution  : function(seckillId,md5){
            return "/will/seckill/"+seckillId+"/"+md5+"/execution";
        }
    },
    validatePhone : function(killPhone){
        if(killPhone!=null && killPhone.length==2 && !isNaN(killPhone)){
            return true ;
        }else{
            return false;
        }
    },
    countdown : function(seckillId,nowTime,startTime,endTime){
        var seckillBox = $('#seckill-box');
        //时间判断
        if(nowTime > endTime){
            //秒杀结束
            seckillBox.html('秒杀结束');
        }else if(nowTime < startTime){
        //    秒杀未开始,计时事件绑定
            var killTime = new Date(startTime+1000);
            seckillBox.countdown(killTime,function(event){
            //    时间格式
                var format = event.strftime('秒杀倒计时：%D天  %H时  %M分  %S秒');
                seckillBox.html(format);
            }).on('finish.countdown',function(){
            //    获取秒杀地址，控制显示逻辑，执行秒杀

            });
        }else{
        //    秒杀开始
            seckill.handleSeckillkill(seckillId,seckillBox);
        }

    },
    handleSeckillkill : function(seckillId,node){
        node.hide().html('<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');

        $.post(seckill.URL.exposer(seckillId),{},function(result){
            if(result && result['success']){
                var exposer = result['data'];
                if(exposer['exposed']){
                    //开启秒杀
                    //获取秒杀地址
                    var md5 = exposer['md5'];
                    var killUrl = seckill.URL.execution(seckillId,md5);
                    $('#killBtn').on('click',function(){
                        $(this).addClass('disabled');
                        $post(killUrl,{},function(result){
                            if(resutl && result['success']){
                                var killResult = result['data'];
                                var state = result['state'];
                                var stateInfo = result['stateInfo'];
                                //    显示秒杀结果
                                node.html('<span class="label label-success">'+stateInfo+' </span>');
                            }
                        });
                    });
                    node.show();
                }else{

                }
            }else{

            }
        });

    },


    //详情秒杀逻辑
    detail : {
        //详情页初始化
        init : function(params) {
            //    手机验证和登录，计时交互
            //    规划我们的交互流程
            //    在cookie中查找手机号
            var killPhone = $.cookie("killPhone");
            var startTime = params["startTime"];
            var endTime = params["endTime"];
            var seckillId = params["seckillId"];
            //    验证手机
            if (!seckill.validatePhone(killPhone)){
                //绑定phone
                //控制输出
                var killPhoneModal =  $("#killPhoneModal");
                //显示弹出层
                killPhoneModal.modal({
                    show : true ,//显示弹出层
                    backdrop : 'static',//禁止位置关闭
                    keyboard : false//关闭键盘事件
                });

                $("#killPhoneBtn").click(function(){
                    var inputPhone = $("#killPhoneKey").val();
                    if(seckill.validatePhone(inputPhone)){
                        //电话写入cookie
                        $.cookie("killPhone",inputPhone,{expires:100,path:"/will"});

                        //刷新页面
                        window.location.reload();
                    }else{
                        $("#killPhoneMessage").hide().html("<label class='label label-danger'>手机号码错误</label>").show(300);
                    }
                });


            }
            //已经登录
            //计时交互
            $.get(seckill.URL.now(),{},function(result){
                if(result && result.success==true){
                    var nowTime = result['data'];
                    console.log(nowTime);
                   seckill.countdown(seckillId,nowTime,startTime,endTime);

                }
            });
        }




    }

};

