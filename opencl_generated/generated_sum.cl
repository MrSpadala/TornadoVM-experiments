#pragma OPENCL EXTENSION cl_khr_fp64 : enable  
__kernel void reductionAddFloats(__global uchar *_heap_base, ulong _frame_base, __constant uchar *_constant_region, __local uchar *_local_region, __global uchar *_private_region)
{
  float f_31, f_30, f_36, f_24;
  int i_21, i_20, i_19, i_18, i_17, i_16, i_15, i_14, i_9, i_41, i_8, i_7, i_6, i_5, i_4, i_3, i_34, i_33, i_32, i_25, i_22;
  bool z_23, z_35;
  ulong ul_40, ul_0, ul_13, ul_29, ul_1, ul_2;
  long l_38, l_39, l_37, l_12, l_28, l_10, l_26, l_11, l_27;

  __global ulong *_frame = (__global ulong *) &_heap_base[_frame_base];

  ul_0  =  (ulong) _frame[6];
  ul_1  =  (ulong) _frame[7];
  ul_2  =  ul_1 + 24L;
  *((__global float *) ul_2)  =  0.0F;
  i_3  =  get_global_id(0);
  i_4  =  i_3;
  //actually this for is executed only once. It acts like an if statement
  for(;i_4 < 8192;)  {
    i_5  =  get_local_id(0);
    i_6  =  get_local_size(0);
    i_7  =  get_group_id(0);
    i_8  =  i_6 * i_7;  
    i_9  =  i_8 + i_5;
    //i_9 contains the offset in work-items from the base
    l_10  =  (long) i_9;
    //l_11 contains the offset in bytes from the base (4 bytes per float)
    l_11  =  l_10 << 2;
    l_12  =  l_11 + 24L;
    //ul_13 is the address in which the thread will write its sum
    ul_13  =  ul_0 + l_12;
    i_14  =  i_6 >> 31;
    i_15  =  i_14 >> 31;
    i_16  =  i_15 + i_6; //i_16 contains the local size
    i_17  =  i_16 >> 1;  //i_17=i_18 contains the local size halved
    i_18  =  i_17;
    /* Reduction. Loop until i_18 is greater or equal than 1. At every
    iteration divide i_18 by 2. Effectively what the code is doing is
    a reduction at work-group level. On the first iteration half the
    elements are summed with the other half. At the second iteration
    half of the already summed elements are summed with the second 
    half (quarters of the initial array). When all the elements are
    summed, i_18 is lower than 1 and this loop exits. */
    for(;i_18 >= 1;) {
      barrier(CLK_LOCAL_MEM_FENCE);
      i_19  =  i_18 >> 31;
      i_20  =  i_19 >> 31;
      i_21  =  i_20 + i_18;
      i_22  =  i_21 >> 1;  //i_22 contains i_18 divided by 2
      z_23  =  i_5 < i_18;
      if(z_23)   //the local_id is less than i_18?
      {
        f_24  =  *((__global float *) ul_13);
        // At the first iteration i_18 is half the local size.
        // i_25 contains the offset from the base in work-items
        // of the second float to sum
        i_25  =  i_9 + i_18; 
        l_26  =  (long) i_25;
        l_27  =  l_26 << 2;
        l_28  =  l_27 + 24L;
        ul_29  =  ul_0 + l_28;  //address of the second float to sum
        f_30  =  *((__global float *) ul_29);
        f_31  =  f_24 + f_30;
        *((__global float *) ul_13)  =  f_31;  //write the result
      }
      else
      {
      }
      i_32  =  i_22;
      i_18  =  i_32;  //assign to i_18 itself divided by two
    }
    barrier(CLK_GLOBAL_MEM_FENCE);
    i_33  =  get_global_size(0);
    i_34  =  i_33 + i_4;
    //z_35 is true if the thread is assigned to the first item in the work group
    z_35  =  i_5 == 0;
    if(z_35)
    {
      // is the thread assigned to the first work item in the work group?
      // if yes, write the sum of this work group to the output array.
      // I suppose that this kernel will be called again on this output array.
      f_36  =  *((__global float *) ul_13);
      l_37  =  (long) i_7;
      l_38  =  l_37 << 2;
      l_39  =  l_38 + 24L;
      ul_40  =  ul_1 + l_39;
      *((__global float *) ul_40)  =  f_36;
    }
    else
    {
    }
    i_41  =  i_34;
    i_4  =  i_41;
  }
  return;
}
